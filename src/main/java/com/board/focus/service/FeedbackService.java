package com.board.focus.service;

import com.board.domain.user.User;
import com.board.domain.user.UserRepository;
import com.board.exception.CustomException;
import com.board.exception.ErrorCode;
import com.board.focus.domain.*;
import com.board.focus.dto.FeedbackRequest;
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
public class FeedbackService {

    private final TaskFeedbackRepository feedbackRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final UserGoalWeightsRepository weightsRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveFeedback(String username, Long taskId, FeedbackRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        DailyTask task = dailyTaskRepository.findById(taskId)
            .orElseThrow(() -> new CustomException(ErrorCode.DAILY_TASK_NOT_FOUND));

        if (feedbackRepository.existsByDailyTaskId(taskId)) {
            return; // 이미 피드백 있음 → 무시 (멱등성)
        }

        FeedbackResult result = FeedbackResult.valueOf(request.result());
        FeedbackReason reason = request.reason() != null
            ? FeedbackReason.valueOf(request.reason())
            : null;

        feedbackRepository.save(TaskFeedback.builder()
            .dailyTask(task)
            .user(user)
            .result(result)
            .reason(reason)
            .build());

        // 태스크 상태 업데이트
        if (result == FeedbackResult.DONE) {
            task.complete();
            if (task.getTaskCandidate() != null) {
                task.getTaskCandidate().updateStatus(CandidateStatus.COMPLETED);
            }
        } else if (reason == FeedbackReason.NO_LONGER_NEEDED) {
            if (task.getTaskCandidate() != null) {
                task.getTaskCandidate().updateStatus(CandidateStatus.DISMISSED);
            }
        }

        // 피드백 30개 이상 쌓이면 가중치 재조정
        recalibrateIfNeeded(user.getId());
    }

    // 30일 피드백 기반 가중치 재조정
    @Transactional
    public void recalibrate(Long userId) {
        UserGoalWeights weights = weightsRepository.findByUserId(userId).orElse(null);
        if (weights == null) return;

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<TaskFeedback> recent = feedbackRepository.findRecentByUserId(userId, since);

        if (recent.size() < 10) return; // 데이터 부족

        // 카테고리별 완료율 계산
        Map<TaskCategory, Double> completionRates = calculateCompletionRates(recent);
        if (completionRates.isEmpty()) return;

        // 완료율이 낮은 카테고리는 가중치 하향, 높은 건 상향
        double[] adjusted = adjustWeights(weights, completionRates);
        weights.adjustWeights(adjusted[0], adjusted[1], adjusted[2], adjusted[3]);

        log.info("Recalibrated weights for userId={}: B={} F={} H={} S={}",
            userId, adjusted[0], adjusted[1], adjusted[2], adjusted[3]);
    }

    private void recalibrateIfNeeded(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long count = feedbackRepository.findRecentByUserId(userId, since).size();
        // 30개마다 재조정
        if (count > 0 && count % 30 == 0) {
            recalibrate(userId);
        }
    }

    private Map<TaskCategory, Double> calculateCompletionRates(List<TaskFeedback> feedbacks) {
        return feedbacks.stream()
            .filter(f -> f.getDailyTask().getTaskCandidate() != null)
            .collect(Collectors.groupingBy(
                f -> f.getDailyTask().getTaskCandidate().getCategory(),
                Collectors.averagingDouble(f -> f.getResult() == FeedbackResult.DONE ? 1.0 : 0.0)
            ));
    }

    private double[] adjustWeights(UserGoalWeights current, Map<TaskCategory, Double> rates) {
        double b = applyRate(current.getBusinessWeight(),    rates.getOrDefault(TaskCategory.BUSINESS,    0.5));
        double f = applyRate(current.getFamilyWeight(),      rates.getOrDefault(TaskCategory.FAMILY,      0.5));
        double h = applyRate(current.getHealthWeight(),      rates.getOrDefault(TaskCategory.HEALTH,      0.5));
        double s = applyRate(current.getSelfGrowthWeight(),  rates.getOrDefault(TaskCategory.SELF_GROWTH, 0.5));

        double sum = b + f + h + s;
        if (sum == 0) return new double[]{0.25, 0.25, 0.25, 0.25};
        return new double[]{b / sum, f / sum, h / sum, s / sum};
    }

    // 완료율이 낮은 카테고리는 가중치 소폭 축소 (사용자가 실제로 하는 것 위주로)
    private double applyRate(double weight, double completionRate) {
        double adjustment = completionRate > 0.7 ? 1.05 : completionRate < 0.3 ? 0.90 : 1.0;
        return Math.max(0.05, weight * adjustment);
    }
}
