package com.board.domain.comment;

import com.board.domain.post.Post;
import com.board.domain.post.PostRepository;
import com.board.domain.user.User;
import com.board.domain.user.UserRepository;
import com.board.dto.comment.*;
import com.board.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse create(Long postId, CommentRequest request, String username) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Comment comment = commentRepository.save(Comment.builder()
            .content(request.getContent())
            .post(post)
            .author(user)
            .build());
        return new CommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getList(Long postId) {
        return commentRepository.findByPost_Id(postId).stream()
            .map(CommentResponse::new).toList();
    }

    @Transactional
    public CommentResponse update(Long id, CommentRequest request, String username) {
        Comment comment = findComment(id);
        checkAuthor(comment, username);
        comment.update(request.getContent());
        return new CommentResponse(comment);
    }

    @Transactional
    public void delete(Long id, String username) {
        Comment comment = findComment(id);
        checkAuthor(comment, username);
        commentRepository.delete(comment);
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void checkAuthor(Comment comment, String username) {
        if (!comment.getAuthor().getUsername().equals(username))
            throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
