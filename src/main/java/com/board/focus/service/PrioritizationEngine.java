package com.board.focus.service;

import com.board.focus.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class PrioritizationEngine {

    // 점수 가중치 (합계 = 1.0)
    private static final double W_GOAL      = 0.40;
    private static final double W_URGENCY   = 0.30;
    private static final double W_AVOIDANCE = 0.20;
    private static final double W_EXECUTION = 0.10;

    public List<TaskCandidate> scoreAndRank(List<TaskCandidate> candidates, UserGoalWeights weights) {
        candidates.forEach(c -> applyScore(c, weights));
        return candidates.stream()
            .sorted(Comparator.comparingDouble(TaskCandidate::getTotalScore).reversed())
            .toList();
    }

    private void applyScore(TaskCandidate c, UserGoalWeights weights) {
        double goalAlignment = goalAlignmentScore(c, weights);
        double urgency       = urgencyScore(c);
        double execution     = executionCostScore(c, weights);
        double total         = (goalAlignment * W_GOAL)
                             + (urgency       * W_URGENCY)
                             + (c.getAvoidanceScore() * W_AVOIDANCE)
                             + (execution     * W_EXECUTION);

        c.applyScore(goalAlignment, urgency, execution, cap(total));
    }

    // 목표 카테고리와 사용자 가중치 매핑
    private double goalAlignmentScore(TaskCandidate c, UserGoalWeights w) {
        if (w == null) return 0.25;
        return switch (c.getCategory()) {
            case BUSINESS    -> w.getBusinessWeight();
            case FAMILY      -> w.getFamilyWeight();
            case HEALTH      -> w.getHealthWeight();
            case SELF_GROWTH -> w.getSelfGrowthWeight();
        };
    }

    // 마감일 기반 긴박도
    private double urgencyScore(TaskCandidate c) {
        if (c.getDeadline() == null) return 0.1;

        long days = ChronoUnit.DAYS.between(LocalDate.now(), c.getDeadline().toLocalDate());

        if (days < 0)  return 0.0;   // 이미 지남 → 페널티 (다음 로직에서 필터)
        if (days == 0) return 1.0;
        if (days <= 1) return 0.90;
        if (days <= 3) return 0.70;
        if (days <= 7) return 0.50;
        if (days <= 14) return 0.30;
        return 0.10;
    }

    // 사용자 선호 소요시간과 태스크 예상시간 매핑
    private double executionCostScore(TaskCandidate c, UserGoalWeights w) {
        if (c.getEstimatedMinutes() == null) return 0.5;
        if (w == null || w.getPreferredDuration() == null) return 0.5;

        int mins = c.getEstimatedMinutes();
        return switch (w.getPreferredDuration()) {
            case SHORT  -> mins <= 30 ? 1.0 : mins <= 60 ? 0.6 : 0.2;
            case MEDIUM -> mins <= 90 ? (mins >= 30 ? 1.0 : 0.7) : 0.4;
            case LONG   -> mins >= 90 ? 1.0 : mins >= 60 ? 0.7 : 0.3;
        };
    }

    private double cap(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
