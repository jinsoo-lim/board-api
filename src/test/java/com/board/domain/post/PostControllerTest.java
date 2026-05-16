package com.board.domain.post;

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
class PostControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "username", "postuser",
                "password", "password123",
                "email", "post@test.com"
            ))));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "postuser",
                    "password", "password123"
                ))))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        token = objectMapper.readTree(body).at("/data/token").asText();
    }

    @Test
    @DisplayName("게시글 작성 - 인증 성공")
    void create_post_with_auth() throws Exception {
        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "테스트 게시글",
                    "content", "테스트 내용입니다."
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("테스트 게시글"))
            .andExpect(jsonPath("$.data.author").value("postuser"));
    }

    @Test
    @DisplayName("게시글 작성 - 인증 없음 → 403")
    void create_post_without_auth() throws Exception {
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "제목",
                    "content", "내용"
                ))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 목록 조회 - 비로그인도 가능")
    void get_post_list_public() throws Exception {
        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("게시글 수정 - 작성자 성공")
    void update_post_by_author() throws Exception {
        // 게시글 작성
        MvcResult createResult = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "원래 제목",
                    "content", "원래 내용"
                ))))
            .andReturn();

        Long postId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .at("/data/id").asLong();

        // 수정
        mockMvc.perform(put("/api/posts/" + postId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "수정된 제목",
                    "content", "수정된 내용"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("게시글 삭제 - 작성자 성공")
    void delete_post_by_author() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "삭제할 게시글",
                    "content", "삭제됩니다"
                ))))
            .andReturn();

        Long postId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .at("/data/id").asLong();

        mockMvc.perform(delete("/api/posts/" + postId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("삭제 완료"));
    }

    @Test
    @DisplayName("게시글 키워드 검색")
    void search_posts_by_keyword() throws Exception {
        mockMvc.perform(post("/api/posts")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "title", "스프링 게시글",
                "content", "내용"
            ))));

        mockMvc.perform(get("/api/posts").param("keyword", "스프링"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].title").value("스프링 게시글"));
    }
}
