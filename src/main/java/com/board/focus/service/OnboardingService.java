package com.board.focus.service;

import com.board.domain.user.User;
import com.board.domain.user.UserRepository;
import com.board.exception.CustomException;
import com.board.exception.ErrorCode;
import com.board.focus.domain.*;
import com.board.focus.dto.OnboardingRequest;
import com.board.focus.dto.OnboardingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserGoalWeightsRepository weightsRepository;
    private final UserRepository userRepository;
    private final ClaudeApiService claudeApiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public OnboardingResponse saveOnboarding(String username, OnboardingRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Claude API로 목표 파싱
        Map<String, Object> parsed = claudeApiService.parseOnboardingGoals(
            request.yearGoalAnswer(),
            request.avoidanceAnswer(),
            request.importantPeopleAnswer()
        );

        double[] weights = extractWeights(parsed);
        FocusPeriod focusPeriod = parseFocusPeriod(request.focusPeriod());
        TaskDuration preferredDuration = parseTaskDuration(request.preferredDuration());

        String yearlyGoalsJson = toJson(parsed.getOrDefault("yearlyGoals", List.of()));
        String avoidanceJson = toJson(parsed.getOrDefault("avoidanceTopics", List.of()));
        String peopleJson = toJson(parsed.getOrDefault("importantPeople", List.of()));

        UserGoalWeights goalWeights = weightsRepository.findByUserId(user.getId())
            .orElse(null);

        if (goalWeights == null) {
            goalWeights = UserGoalWeights.builder()
                .user(user)
                .businessWeight(weights[0])
                .familyWeight(weights[1])
                .healthWeight(weights[2])
                .selfGrowthWeight(weights[3])
                .focusPeriod(focusPeriod)
                .preferredDuration(preferredDuration)
                .wakeTime(request.wakeTime())
                .yearlyGoalsJson(yearlyGoalsJson)
                .avoidanceTopicsJson(avoidanceJson)
                .importantPeopleJson(peopleJson)
                .build();
        } else {
            goalWeights.update(
                weights[0], weights[1], weights[2], weights[3],
                focusPeriod, preferredDuration, request.wakeTime(),
                yearlyGoalsJson, avoidanceJson, peopleJson
            );
        }

        return OnboardingResponse.from(weightsRepository.save(goalWeights));
    }

    @Transactional(readOnly = true)
    public OnboardingResponse getOnboarding(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return weightsRepository.findByUserId(user.getId())
            .map(OnboardingResponse::from)
            .orElse(null);
    }

    // Claude 응답에서 카테고리 가중치 추출
    @SuppressWarnings("unchecked")
    private double[] extractWeights(Map<String, Object> parsed) {
        try {
            Map<String, Object> cats = (Map<String, Object>) parsed.get("categories");
            if (cats == null) return defaultWeights();

            double business = toDouble(cats.get("BUSINESS"), 0.4);
            double family = toDouble(cats.get("FAMILY"), 0.3);
            double health = toDouble(cats.get("HEALTH"), 0.2);
            double selfGrowth = toDouble(cats.get("SELF_GROWTH"), 0.1);

            // 합계 정규화
            double sum = business + family + health + selfGrowth;
            if (sum == 0) return defaultWeights();

            return new double[]{business / sum, family / sum, health / sum, selfGrowth / sum};
        } catch (Exception e) {
            log.warn("Weight extraction failed, using defaults: {}", e.getMessage());
            return defaultWeights();
        }
    }

    private double[] defaultWeights() {
        return new double[]{0.4, 0.3, 0.2, 0.1};
    }

    private double toDouble(Object val, double fallback) {
        if (val == null) return fallback;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return fallback; }
    }

    private FocusPeriod parseFocusPeriod(String val) {
        try { return FocusPeriod.valueOf(val.toUpperCase()); }
        catch (Exception e) { return FocusPeriod.MORNING; }
    }

    private TaskDuration parseTaskDuration(String val) {
        try { return TaskDuration.valueOf(val.toUpperCase()); }
        catch (Exception e) { return TaskDuration.MEDIUM; }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }
}
