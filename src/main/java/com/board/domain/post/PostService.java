package com.board.domain.post;

import com.board.domain.user.User;
import com.board.domain.user.UserRepository;
import com.board.dto.post.*;
import com.board.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostResponse create(PostRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Post post = postRepository.save(Post.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .author(user)
            .build());
        return new PostResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getList(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank())
            return postRepository.findByTitleContaining(keyword, pageable).map(PostResponse::new);
        return postRepository.findAll(pageable).map(PostResponse::new);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id) {
        return new PostResponse(findPost(id));
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request, String username) {
        Post post = findPost(id);
        checkAuthor(post, username);
        post.update(request.getTitle(), request.getContent());
        return new PostResponse(post);
    }

    @Transactional
    public void delete(Long id, String username) {
        Post post = findPost(id);
        checkAuthor(post, username);
        postRepository.delete(post);
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private void checkAuthor(Post post, String username) {
        if (!post.getAuthor().getUsername().equals(username))
            throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
