package com.example.onboarding.service;

import com.example.onboarding.dto.ClaudeAnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Claude API 통합 서비스
 * - Anthropic Claude API를 사용하여 대화 내용 분석
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.max-tokens}")
    private int maxTokens;

    @Value("${anthropic.temperature}")
    private double temperature;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /**
     * 대화 내용을 분석하여 맛집 추천 필요 여부 판단
     *
     * @param conversationContext 최근 대화 기록
     * @param currentMessage 현재 메시지
     * @return Claude 분석 결과
     */
    public ClaudeAnalysisResult analyzeConversation(
            List<String> conversationContext,
            String currentMessage) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(conversationContext, currentMessage);

            log.debug("Sending request to Claude API");

            // Claude API 요청 본문 생성
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "temperature", temperature,
                    "system", systemPrompt,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", userPrompt
                            )
                    )
            );

            // WebClient로 Claude API 호출
            WebClient webClient = WebClient.builder()
                    .baseUrl(CLAUDE_API_URL)
                    .defaultHeader("x-api-key", apiKey)
                    .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                    .defaultHeader("content-type", "application/json")
                    .build();

            String responseBody = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Claude API response received");

            // JSON 파싱
            JsonNode responseJson = objectMapper.readTree(responseBody);
            String claudeResponse = responseJson.get("content").get(0).get("text").asText();

            log.info("Claude analysis response: {}", claudeResponse);

            return parseClaudeResponse(claudeResponse);

        } catch (Exception e) {
            log.error("Failed to analyze conversation with Claude", e);
            return ClaudeAnalysisResult.builder()
                    .shouldRecommend(false)
                    .confidence(0.0)
                    .reasoning("Error occurred during analysis: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Claude에게 제공할 시스템 프롬프트 생성
     */
    private String buildSystemPrompt() {
        return """
                당신은 한국어 대화를 분석하여 맛집 추천이 필요한지 판단하는 AI 어시스턴트입니다.

                당신의 역할:
                1. 사용자들의 대화를 분석하여 식사 또는 맛집과 관련된 의도를 파악합니다
                2. 다음 정보를 추출합니다:
                   - 장소/지역 (예: 판교, 강남, 잠실, 건대, 합정)
                   - 식사 종류 (예: 점심, 저녁, 브런치, 야식)
                   - 음식 카테고리 선호도 (예: 한식, 일식, 중식, 양식, 카페)
                   - 기타 선호사항 (예: 회식, 데이트, 분위기, 저렴한)
                3. 추천 여부와 신뢰도를 결정합니다

                추천이 필요한 경우:
                - "어디서 밥 먹을까?", "뭐 먹지?", "맛집 추천해줘" 같은 직접적인 요청
                - "배고파", "점심 때 됐다" 같은 간접적인 식사 의도
                - 특정 지역과 식사를 함께 언급 (예: "판교에서 점심")

                추천이 불필요한 경우:
                - 일반 대화나 인사
                - 음식과 무관한 주제
                - 이미 식사를 마친 경우

                응답 형식은 반드시 JSON으로 작성하세요:
                {
                  "shouldRecommend": boolean,
                  "location": "string or null",
                  "mealType": "string or null",
                  "categories": ["string"],
                  "preferences": ["string"],
                  "confidence": 0.0-1.0,
                  "reasoning": "string"
                }

                중요: JSON 외에 다른 텍스트는 절대 포함하지 마세요.
                """;
    }

    /**
     * 사용자 프롬프트 생성
     */
    private String buildUserPrompt(List<String> context, String currentMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("대화 기록:\n");

        if (context != null && !context.isEmpty()) {
            for (String msg : context) {
                prompt.append("- ").append(msg).append("\n");
            }
        } else {
            prompt.append("(대화 기록 없음)\n");
        }

        prompt.append("\n현재 메시지: \"").append(currentMessage).append("\"\n\n");
        prompt.append("이 대화를 분석하여 맛집 추천이 필요한지 판단하고 JSON 형식으로 응답해주세요.");

        return prompt.toString();
    }

    /**
     * Claude 응답을 ClaudeAnalysisResult로 파싱
     */
    private ClaudeAnalysisResult parseClaudeResponse(String response) {
        try {
            // JSON 부분만 추출 (```json ``` 마크다운 제거)
            String jsonString = response.trim();
            if (jsonString.startsWith("```json")) {
                jsonString = jsonString.substring(7);
            }
            if (jsonString.startsWith("```")) {
                jsonString = jsonString.substring(3);
            }
            if (jsonString.endsWith("```")) {
                jsonString = jsonString.substring(0, jsonString.length() - 3);
            }
            jsonString = jsonString.trim();

            return objectMapper.readValue(jsonString, ClaudeAnalysisResult.class);

        } catch (Exception e) {
            log.error("Failed to parse Claude response as JSON: {}", response, e);

            // Fallback: 키워드 기반 감지
            boolean shouldRecommend = containsKeywords(response);

            return ClaudeAnalysisResult.builder()
                    .shouldRecommend(shouldRecommend)
                    .confidence(0.5)
                    .reasoning("Fallback keyword detection (JSON parsing failed)")
                    .categories(Collections.emptyList())
                    .preferences(Collections.emptyList())
                    .build();
        }
    }

    /**
     * Fallback: 키워드 기반 추천 필요 여부 판단
     */
    private boolean containsKeywords(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("shouldrecommend") &&
               (lowerText.contains("true") || lowerText.contains("추천"));
    }
}
