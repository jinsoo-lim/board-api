package com.board.focus.dto;

import com.board.focus.domain.DailyTask;

public record DailyTaskResponse(
    Long taskId,
    String description,
    String reason,            // 중간 신뢰도일 때만 표시
    double confidenceScore,
    boolean requiresChoice,   // 낮은 신뢰도 → 사용자가 2개 중 선택
    DailyTaskResponse alternative,
    String status
) {
    public static DailyTaskResponse from(DailyTask task) {
        return new DailyTaskResponse(
            task.getId(),
            task.getDescription(),
            task.getReason(),
            task.getConfidenceScore(),
            task.getAlternativeJson() != null,
            null,
            task.getStatus().name()
        );
    }

    public static DailyTaskResponse noTask() {
        return new DailyTaskResponse(
            null,
            "메모를 더 추가해주세요. 내일 제안드릴게요.",
            null, 0.0, false, null, "INSUFFICIENT"
        );
    }
}
