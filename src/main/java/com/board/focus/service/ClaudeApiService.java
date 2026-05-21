package com.board.focus.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class ClaudeApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${focus.claude.api-key}")
    private String apiKey;

    @Value("${focus.claude.api-url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;

    @Value("${focus.claude.model:claude-opus-4-7}")
    private String model;

    public ClaudeApiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // 메모 분석 → 태스크 후보 JSON 리스트 반환
    public List<Map<String, Object>> analyzeMemos(String memosText, String goalsContext) {
        String prompt = buildMemoAnalysisPrompt(memosText, goalsContext);
        String response = callClaude(prompt, 2048);
        return parseCandidateList(response);
    }

    // 온보딩 답변 파싱 → 목표 구조화 JSON 반환
    public Map<String, Object> parseOnboardingGoals(String yearGoal, String avoidance, String importantPeople) {
        String prompt = buildOnboardingPrompt(yearGoal, avoidance, importantPeople);
        String response = callClaude(prompt, 1024);
        return parseGoalMap(response);
    }

    private String callClaude(String prompt, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            return "[]";
        }
    }

    private String buildMemoAnalysisPrompt(String memos, String goals) {
        return """
            당신은 개인 메모를 분석해 실행 가능한 태스크를 추출하는 AI입니다.
            반드시 유효한 JSON 배열만 반환하세요. 설명 텍스트 없이 JSON만.

            사용자 연간 목표:
            %s

            최근 메모:
            %s

            아래 형식의 JSON 배열로 반환 (태스크가 없으면 빈 배열 []):
            [
              {
                "description": "구체적인 행동",
                "category": "BUSINESS|FAMILY|HEALTH|SELF_GROWTH",
                "estimatedMinutes": 30,
                "deadlineDays": null,
                "mentionCount": 1,
                "hasAvoidanceSignal": false,
                "relatedPeople": []
              }
            ]

            규칙:
            - description은 오늘 바로 실행 가능한 구체적 행동으로
            - category는 정확히 4가지 중 하나
            - deadlineDays는 마감일이 있으면 숫자, 없으면 null
            - hasAvoidanceSignal은 "계속 미루고 있다", "해야 하는데" 같은 표현이 있으면 true
            - 동일 주제의 메모는 하나의 태스크로 통합하고 mentionCount 증가
            """.formatted(goals, memos);
    }

    private String buildOnboardingPrompt(String yearGoal, String avoidance, String importantPeople) {
        return """
            사용자의 온보딩 답변을 분석해 목표 구조를 추출하세요.
            반드시 유효한 JSON 객체만 반환하세요. 설명 없이 JSON만.

            Q1. 1년 후 성공의 모습:
            %s

            Q2. 현재 피하고 있는 것:
            %s

            Q3. 중요한 사람들:
            %s

            아래 형식으로 반환:
            {
              "categories": {
                "BUSINESS": 0.5,
                "FAMILY": 0.3,
                "HEALTH": 0.1,
                "SELF_GROWTH": 0.1
              },
              "yearlyGoals": ["목표1", "목표2"],
              "avoidanceTopics": ["주제1"],
              "importantPeople": ["이름1", "이름2"]
            }

            규칙:
            - categories의 합계는 반드시 1.0
            - 답변 내용을 보고 비중을 자동 판단
            - yearlyGoals는 구체적인 목표만 (최대 5개)
            """.formatted(yearGoal, avoidance, importantPeople);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseCandidateList(String json) {
        try {
            String cleaned = extractJson(json, "[");
            return objectMapper.readValue(cleaned, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse candidate list from Claude response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGoalMap(String json) {
        try {
            String cleaned = extractJson(json, "{");
            return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse goal map from Claude response: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String extractJson(String text, String startChar) {
        int start = text.indexOf(startChar);
        if (start < 0) return startChar.equals("[") ? "[]" : "{}";
        String end = startChar.equals("[") ? "]" : "}";
        int last = text.lastIndexOf(end);
        if (last < 0) return startChar.equals("[") ? "[]" : "{}";
        return text.substring(start, last + 1);
    }
}
