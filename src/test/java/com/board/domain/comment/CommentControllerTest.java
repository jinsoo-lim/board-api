package com.board.domain.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "username", "commentuser",
                "password", "password123",
                "email", "comment@test.com"
            ))));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "commentuser",
                    "password", "password123"
                ))))
            .andReturn();
        token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).at("/data/token").asText();

        MvcResult postResult = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "테스트 게시글",
                    "content", "내용"
                ))))
            .andReturn();
        postId = objectMapper.readTree(postResult.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    @Test
    @DisplayName("댓글 작성 - 인증 성공")
    void create_comment_with_auth() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "테스트 댓글"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").value("테스트 댓글"))
            .andExpect(jsonPath("$.data.author").value("commentuser"));
    }

    @Test
    @DisplayName("댓글 작성 - 인증 없음 → 403")
    void create_comment_without_auth() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "댓글"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("댓글 목록 조회 - 비로그인도 가능")
    void get_comment_list_public() throws Exception {
        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("댓글 수정 - 작성자 성공")
    void update_comment_by_author() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "원래 댓글"))))
            .andReturn();

        Long commentId = objectMapper.readTree(createResult.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(put("/api/posts/" + postId + "/comments/" + commentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "수정된 댓글"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").value("수정된 댓글"));
    }

    @Test
    @DisplayName("댓글 삭제 - 작성자 성공")
    void delete_comment_by_author() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "삭제할 댓글"))))
            .andReturn();

        Long commentId = objectMapper.readTree(createResult.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(delete("/api/posts/" + postId + "/comments/" + commentId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("삭제 완료"));
    }
}
