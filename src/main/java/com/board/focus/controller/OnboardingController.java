package com.board.focus.controller;

import com.board.dto.ApiResponse;
import com.board.focus.dto.OnboardingRequest;
import com.board.focus.dto.OnboardingResponse;
import com.board.focus.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/focus/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> save(
            @Valid @RequestBody OnboardingRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
            onboardingService.saveOnboarding(user.getUsername(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> get(
            @AuthenticationPrincipal UserDetails user) {
        OnboardingResponse response = onboardingService.getOnboarding(user.getUsername());
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.error("온보딩을 완료해주세요."));
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
