package com.board.domain.comment;

import com.board.domain.post.Post;
import com.board.domain.post.PostRepository;
import com.board.domain.user.User;
import com.board.domain.user.UserRepository;
import com.board.dto.comment.CommentRequest;
import com.board.dto.comment.CommentResponse;
import com.board.exception.CustomException;
import com.board.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks CommentService commentService;
    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    private User author;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        author = User.builder().username("author").password("pw").email("a@t.com").build();
        post = Post.builder().title("제목").content("내용").author(author).build();
        comment = Comment.builder().content("댓글 내용").post(post).author(author).build();
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void create_success() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findByUsername("author")).willReturn(Optional.of(author));
        given(commentRepository.save(any())).willReturn(comment);

        CommentResponse res = commentService.create(1L, mockRequest("댓글 내용"), "author");

        assertThat(res.getContent()).isEqualTo("댓글 내용");
        assertThat(res.getAuthor()).isEqualTo("author");
    }

    @Test
    @DisplayName("댓글 작성 실패 - 없는 게시글")
    void create_post_not_found() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(99L, mockRequest("내용"), "author"))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 목록 조회")
    void getList_success() {
        given(commentRepository.findByPost_Id(1L)).willReturn(List.of(comment));

        List<CommentResponse> list = commentService.getList(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getContent()).isEqualTo("댓글 내용");
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void update_success() {
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        CommentResponse res = commentService.update(1L, mockRequest("수정된 댓글"), "author");

        assertThat(res.getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("댓글 수정 실패 - 권한 없음")
    void update_unauthorized() {
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(1L, mockRequest("수정"), "other"))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void delete_success() {
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        commentService.delete(1L, "author");

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 권한 없음")
    void delete_unauthorized() {
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(1L, "other"))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private CommentRequest mockRequest(String content) {
        try {
            CommentRequest req = new CommentRequest();
            var f = CommentRequest.class.getDeclaredField("content");
            f.setAccessible(true);
            f.set(req, content);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
