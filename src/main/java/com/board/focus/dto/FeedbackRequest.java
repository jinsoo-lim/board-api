package com.board.focus.dto;

import jakarta.validation.constraints.NotBlank;

public record FeedbackRequest(
    @NotBlank String result,  // DONE / NOT_DONE
    String reason             // TOO_BUSY / DISLIKED / NOT_READY / NO_LONGER_NEEDED (nullable)
) {}
