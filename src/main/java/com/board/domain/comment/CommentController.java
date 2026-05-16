package com.board.domain.comment;

import com.board.dto.ApiResponse;
import com.board.dto.comment.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.create(postId, request, user.getUsername())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getList(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getList(postId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> update(
            @PathVariable Long postId,
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.update(id, request, user.getUsername())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> delete(
            @PathVariable Long postId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        commentService.delete(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("삭제 완료"));
    }
}
