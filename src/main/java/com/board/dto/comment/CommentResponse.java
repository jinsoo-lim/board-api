package com.board.dto.comment;

import com.board.domain.comment.Comment;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class CommentResponse {
    private Long id;
    private String content;
    private String author;
    private LocalDateTime createdAt;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.author = comment.getAuthor().getUsername();
        this.createdAt = comment.getCreatedAt();
    }
}
