package com.board.focus.service;

import com.board.focus.domain.TaskCandidate;
import com.board.focus.domain.UserGoalWeights;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfidenceGate {

    // 신뢰도 임계값
    private static final double HIGH   = 0.90;
    private static final double MEDIUM = 0.75;
    private static final double LOW    = 0.60;

    public enum ConfidenceLevel { HIGH, MEDIUM, LOW, INSUFFICIENT }

    public record ConfidenceResult(
        ConfidenceLevel level,
        double score,
        TaskCandidate primary,
        TaskCandidate alternative  // LOW 구간에서만 non-null
    ) {
        public static ConfidenceResult high(TaskCandidate p, double s) {
            return new ConfidenceResult(ConfidenceLevel.HIGH, s, p, null);
        }
        public static ConfidenceResult medium(TaskCandidate p, double s) {
            return new ConfidenceResult(ConfidenceLevel.MEDIUM, s, p, null);
        }
        public static ConfidenceResult low(TaskCandidate p, TaskCandidate alt, double s) {
            return new ConfidenceResult(ConfidenceLevel.LOW, s, p, alt);
        }
        public static ConfidenceResult insufficient(double s) {
            return new ConfidenceResult(ConfidenceLevel.INSUFFICIENT, s, null, null);
        }
    }

    public ConfidenceResult evaluate(List<TaskCandidate> ranked, UserGoalWeights weights) {
        if (ranked == null || ranked.isEmpty()) {
            return ConfidenceResult.insufficient(0.0);
        }

        TaskCandidate top = ranked.get(0);
        double confidence = calculate(top, ranked, weights);

        // 절대 거부 조건
        if (failsHardGate(top)) {
            return ConfidenceResult.insufficient(confidence);
        }

        if (confidence >= HIGH) {
            return ConfidenceResult.high(top, confidence);
        }
        if (confidence >= MEDIUM) {
            return ConfidenceResult.medium(top, confidence);
        }
        if (confidence >= LOW) {
            TaskCandidate alt = ranked.size() > 1 ? ranked.get(1) : null;
            return ConfidenceResult.low(top, alt, confidence);
        }

        return ConfidenceResult.insufficient(confidence);
    }

    private double calculate(TaskCandidate top, List<TaskCandidate> all, UserGoalWeights weights) {
        double base = top.getTotalScore();

        // 메모 언급 횟수 (3회면 충분)
        double dataBonus = Math.min(top.getMentionCount() / 3.0, 1.0) * 0.10;

        // 회피 신호 보너스 (계속 미루던 거 → 신뢰도 상승)
        double avoidanceBonus = top.getAvoidanceScore() > 0.5 ? 0.05 : 0.0;

        // 경쟁 후보와 점수 차이가 거의 없을 때만 패널티
        double gapPenalty = 0.0;
        if (all.size() > 1) {
            double gap = top.getTotalScore() - all.get(1).getTotalScore();
            if (gap < 0.03) gapPenalty = -0.10;
        }

        // 온보딩 미완료 패널티
        double onboardingPenalty = (weights == null || !weights.isOnboardingComplete()) ? -0.20 : 0.0;

        double raw = (base * 0.85) + dataBonus + avoidanceBonus + gapPenalty + onboardingPenalty;
        return Math.max(0.0, Math.min(1.0, raw));
    }

    // 어떤 상황에서도 출력하면 안 되는 케이스
    private boolean failsHardGate(TaskCandidate top) {
        // 마감일이 이미 지난 태스크 단독 1위
        if (top.getDeadline() != null) {
            java.time.LocalDate deadline = top.getDeadline().toLocalDate();
            if (deadline.isBefore(java.time.LocalDate.now())) {
                return true;
            }
        }
        // 설명이 비어있는 태스크
        if (top.getDescription() == null || top.getDescription().isBlank()) {
            return true;
        }
        return false;
    }
}
