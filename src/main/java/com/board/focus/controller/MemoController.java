package com.board.focus.controller;

import com.board.dto.ApiResponse;
import com.board.focus.dto.MemoRequest;
import com.board.focus.dto.MemoResponse;
import com.board.focus.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/focus/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @PostMapping
    public ResponseEntity<ApiResponse<MemoResponse>> save(
            @Valid @RequestBody MemoRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
            memoService.save(user.getUsername(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemoResponse>>> getAll(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
            memoService.getAll(user.getUsername())));
    }
}
