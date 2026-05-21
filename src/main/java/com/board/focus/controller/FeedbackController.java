package com.board.focus.controller;

import com.board.dto.ApiResponse;
import com.board.focus.dto.FeedbackRequest;
import com.board.focus.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/focus/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    // 저녁 피드백: 했어 / 못했어 + 이유
    @PostMapping("/{taskId}")
    public ResponseEntity<ApiResponse<?>> submit(
            @PathVariable Long taskId,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal UserDetails user) {
        feedbackService.saveFeedback(user.getUsername(), taskId, request);
        return ResponseEntity.ok(ApiResponse.ok("피드백 저장 완료"));
    }
}
