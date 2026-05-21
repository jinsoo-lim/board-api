package com.board.focus.dto;

import jakarta.validation.constraints.NotBlank;

public record MemoRequest(@NotBlank String content) {}
