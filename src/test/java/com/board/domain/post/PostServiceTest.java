package com.board.domain.post;

import com.board.domain.user.User;
import com.board.domain.user.UserRepository;
import com.board.dto.post.PostRequest;
import com.board.dto.post.PostResponse;
import com.board.exception.CustomException;
import com.board.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks PostService postService;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        author = User.builder().username("author").password("pw").email("a@t.com").build();
        post = Post.builder().title("제목").content("내용").author(author).build();
    }

    @Test
    @DisplayName("게시글 작성 성공")
    void create_success() {
        PostRequest req = mockPostRequest("제목", "내용");
        given(userRepository.findByUsername("author")).willReturn(Optional.of(author));
        given(postRepository.save(any())).willReturn(post);

        PostResponse res = postService.create(req, "author");

        assertThat(res.getTitle()).isEqualTo("제목");
        assertThat(res.getAuthor()).isEqualTo("author");
    }

    @Test
    @DisplayName("게시글 목록 조회 - 키워드 없음")
    void getList_no_keyword() {
        var pageable = PageRequest.of(0, 10);
        given(postRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(post)));

        var page = postService.getList(null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("게시글 목록 조회 - 키워드 검색")
    void getList_with_keyword() {
        var pageable = PageRequest.of(0, 10);
        given(postRepository.findByTitleContaining("제목", pageable)).willReturn(new PageImpl<>(List.of(post)));

        var page = postService.getList("제목", pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void get_success() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        PostResponse res = postService.get(1L);

        assertThat(res.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("게시글 단건 조회 실패 - 없는 게시글")
    void get_not_found() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.get(99L))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void update_success() {
        PostRequest req = mockPostRequest("수정제목", "수정내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        PostResponse res = postService.update(1L, req, "author");

        assertThat(res.getTitle()).isEqualTo("수정제목");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음")
    void update_unauthorized() {
        PostRequest req = mockPostRequest("수정제목", "수정내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(1L, req, "other"))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void delete_success() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        postService.delete(1L, "author");

        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 권한 없음")
    void delete_unauthorized() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.delete(1L, "other"))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private PostRequest mockPostRequest(String title, String content) {
        try {
            PostRequest req = new PostRequest();
            setField(req, "title", title);
            setField(req, "content", content);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object obj, String field, String value) throws Exception {
        var f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, value);
    }
}
