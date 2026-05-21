package com.board.focus.service;

import com.board.domain.user.User;
import com.board.domain.user.UserRepository;
import com.board.exception.CustomException;
import com.board.exception.ErrorCode;
import com.board.focus.domain.*;
import com.board.focus.dto.DailyTaskResponse;
import com.board.focus.service.ConfidenceGate.ConfidenceLevel;
import com.board.focus.service.ConfidenceGate.ConfidenceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyTaskService {

    private final DailyTaskRepository dailyTaskRepository;
    private final TaskCandidateRepository candidateRepository;
    private final MemoRepository memoRepository;
    private final UserGoalWeightsRepository weightsRepository;
    private final UserRepository userRepository;
    private final ClaudeApiService claudeApiService;
    private final PrioritizationEngine prioritizationEngine;
    private final ConfidenceGate confidenceGate;
    private final ObjectMapper objectMapper;

    @Transactional
    public DailyTaskResponse getTodayTask(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return dailyTaskRepository.findByUserIdAndTaskDate(user.getId(), LocalDate.now())
            .map(DailyTaskResponse::from)
            .orElseGet(() -> generateForUser(user.getId()));
    }

    // 오늘 태스크 생성 (스케줄러 또는 수동 트리거)
    @Transactional
    public DailyTaskResponse generateForUser(Long userId) {
        // 이미 오늘 생성됐으면 그대로 반환
        return dailyTaskRepository.findByUserIdAndTaskDate(userId, LocalDate.now())
            .map(DailyTaskResponse::from)
            .orElseGet(() -> createDailyTask(userId));
    }

    private DailyTaskResponse createDailyTask(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserGoalWeights weights = weightsRepository.findByUserId(userId).orElse(null);
        if (weights == null || !weights.isOnboardingComplete()) {
            log.info("userId={} has not completed onboarding, skipping", userId);
            return DailyTaskResponse.noTask();
        }

        // 미분석 메모가 있으면 Claude로 분석해 후보 추가
        analyzeNewMemos(user, weights);

        // 현재 PENDING 후보 목록 가져와서 점수 계산
        List<TaskCandidate> candidates = candidateRepository
            .findByUserIdAndStatusOrderByTotalScoreDesc(userId, CandidateStatus.PENDING);

        if (candidates.isEmpty()) {
            return DailyTaskResponse.noTask();
        }

        List<TaskCandidate> ranked = prioritizationEngine.scoreAndRank(candidates, weights);
        ConfidenceResult result = confidenceGate.evaluate(ranked, weights);

        if (result.level() == ConfidenceLevel.INSUFFICIENT) {
            log.info("userId={} confidence insufficient: {}", userId, result.score());
            return DailyTaskResponse.noTask();
        }

        DailyTask task = buildDailyTask(user, result);
        dailyTaskRepository.save(task);
        result.primary().updateStatus(CandidateStatus.SELECTED_TODAY);

        return buildResponse(task, result);
    }

    private void analyzeNewMemos(User user, UserGoalWeights weights) {
        List<Memo> unanalyzed = memoRepository
            .findByUserIdAndAnalyzedFalseOrderByCreatedAtAsc(user.getId());

        if (unanalyzed.isEmpty()) return;

        String memosText = unanalyzed.stream()
            .map(m -> "- " + m.getContent())
            .collect(Collectors.joining("\n"));

        String goalsContext = weights.getYearlyGoalsJson();

        List<Map<String, Object>> extracted = claudeApiService.analyzeMemos(memosText, goalsContext);

        for (Map<String, Object> item : extracted) {
            saveCandidateFromAnalysis(user, item);
        }

        unanalyzed.forEach(Memo::markAnalyzed);
        log.info("userId={} analyzed {} memos, extracted {} candidates",
            user.getId(), unanalyzed.size(), extracted.size());
    }

    private void saveCandidateFromAnalysis(User user, Map<String, Object> item) {
        try {
            String description = (String) item.get("description");
            TaskCategory category = TaskCategory.valueOf(
                item.getOrDefault("category", "BUSINESS").toString());

            double avoidanceScore = Boolean.TRUE.equals(item.get("hasAvoidanceSignal")) ? 0.8 : 0.2;
            int estimatedMinutes = toInt(item.get("estimatedMinutes"), 30);
            int mentionCount = toInt(item.get("mentionCount"), 1);

            Integer deadlineDays = item.get("deadlineDays") != null
                ? toInt(item.get("deadlineDays"), 0) : null;
            LocalDateTime deadline = deadlineDays != null
                ? LocalDateTime.now().plusDays(deadlineDays) : null;

            candidateRepository.save(TaskCandidate.builder()
                .user(user)
                .description(description)
                .category(category)
                .avoidanceScore(avoidanceScore)
                .estimatedMinutes(estimatedMinutes)
                .deadline(deadline)
                .mentionCount(mentionCount)
                .build());
        } catch (Exception e) {
            log.warn("Failed to save candidate from analysis: {}", e.getMessage());
        }
    }

    private DailyTask buildDailyTask(User user, ConfidenceResult result) {
        String reason = result.level() == ConfidenceLevel.MEDIUM
            ? buildReason(result.primary())
            : null;

        String alternativeJson = null;
        if (result.level() == ConfidenceLevel.LOW && result.alternative() != null) {
            alternativeJson = toJson(Map.of("description", result.alternative().getDescription()));
        }

        return DailyTask.builder()
            .user(user)
            .taskCandidate(result.primary())
            .taskDate(LocalDate.now())
            .description(result.primary().getDescription())
            .reason(reason)
            .confidenceScore(result.score())
            .alternativeJson(alternativeJson)
            .build();
    }

    private String buildReason(TaskCandidate c) {
        if (c.getMentionCount() > 3) return c.getMentionCount() + "번 언급된 항목입니다.";
        if (c.getDeadline() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), c.getDeadline().toLocalDate());
            return days + "일 후 마감입니다.";
        }
        return null;
    }

    private DailyTaskResponse buildResponse(DailyTask task, ConfidenceResult result) {
        DailyTaskResponse altResponse = null;
        if (result.alternative() != null) {
            altResponse = new DailyTaskResponse(
                null, result.alternative().getDescription(),
                null, 0.0, false, null, "ALTERNATIVE"
            );
        }
        return new DailyTaskResponse(
            task.getId(), task.getDescription(), task.getReason(),
            task.getConfidenceScore(), result.level() == ConfidenceLevel.LOW,
            altResponse, task.getStatus().name()
        );
    }

    private int toInt(Object val, int fallback) {
        if (val == null) return fallback;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return fallback; }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return null; }
    }
}
