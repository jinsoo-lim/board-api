package com.board.focus.dto;

import com.board.focus.domain.Memo;
import java.time.LocalDateTime;

public record MemoResponse(Long id, String content, boolean analyzed, LocalDateTime createdAt) {
    public static MemoResponse from(Memo m) {
        return new MemoResponse(m.getId(), m.getContent(), m.isAnalyzed(), m.getCreatedAt());
    }
}
