package com.board.dto.post;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class PostRequest {
    @NotBlank @Size(max = 200)
    private String title;

    @NotBlank
    private String content;
}
