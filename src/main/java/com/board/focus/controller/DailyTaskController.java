package com.board.focus.controller;

import com.board.dto.ApiResponse;
import com.board.focus.dto.DailyTaskResponse;
import com.board.focus.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/focus/today")
@RequiredArgsConstructor
public class DailyTaskController {

    private final DailyTaskService dailyTaskService;

    // 오늘의 태스크 조회 (없으면 즉시 생성 시도)
    @GetMapping
    public ResponseEntity<ApiResponse<DailyTaskResponse>> getToday(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
            dailyTaskService.getTodayTask(user.getUsername())));
    }
}
