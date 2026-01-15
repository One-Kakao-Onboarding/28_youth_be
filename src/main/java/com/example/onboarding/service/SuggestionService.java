package com.example.onboarding.service;

import com.example.onboarding.dto.RestaurantDto;
import com.example.onboarding.dto.SuggestionDto;
import com.example.onboarding.entity.ChatMessage;
import com.example.onboarding.entity.Restaurant;
import com.example.onboarding.repository.RestaurantRepository;
import com.example.onboarding.websocket.UserSessionChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 맛집 추천 서비스
 * - LLM 분석 및 맛집 추천 (비동기 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final RestaurantRepository restaurantRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionChannelInterceptor sessionInterceptor;

    /**
     * 메시지 분석 및 맛집 추천 (비동기)
     * - LLM에게 메시지를 보내 의도를 파악하고 맛집을 추천
     * - 추천 결과를 해당 사용자에게만 Private으로 전송
     *
     * @param message 분석할 메시지
     */
    @Async
    public void analyzeMessageAndSuggest(ChatMessage message) {
        try {
            log.info("Starting LLM analysis for message: {}", message.getId());

            // TODO: 여기서 LLM에게 메시지를 보내 의도를 파악하고, 맛집을 추천함
            // 1. LLM API 호출 (예: OpenAI, Anthropic Claude 등)
            // 2. 메시지 내용 분석 (맛집 관련 키워드 추출)
            // 3. 추출된 키워드로 맛집 검색
            // 4. LLM에게 검색 결과를 기반으로 추천 메시지 생성 요청

            // 임시 구현: 메시지에서 간단한 키워드 추출 (판교, 잠실, 합정)
            String content = message.getContent().toLowerCase();
            String keyword = extractKeyword(content);

            if (keyword != null) {
                // 키워드로 맛집 검색
                List<Restaurant> restaurants = restaurantRepository.findByKeywordsContaining(keyword);

                if (!restaurants.isEmpty()) {
                    // 최대 5개까지만 추천
                    List<Restaurant> limitedRestaurants = restaurants.stream()
                            .limit(5)
                            .collect(Collectors.toList());

                    // 사용자에게 Private 메시지로 전송
                    sendSuggestionToUser(message.getSenderId().toString(), limitedRestaurants);

                    log.info("Suggestion sent to user: {}, keyword: {}, count: {}",
                            message.getSenderId(), keyword, limitedRestaurants.size());
                }
            }

        } catch (Exception e) {
            log.error("Failed to analyze message and suggest restaurants", e);
        }
    }

    /**
     * 특정 사용자에게 맛집 추천 전송 (Private Notification)
     * - SimpMessagingTemplate.convertAndSendToUser를 활용하여 특정 사용자에게만 전송
     *
     * @param userId 대상 사용자 ID
     * @param restaurants 추천 맛집 리스트
     */
    private void sendSuggestionToUser(String userId, List<Restaurant> restaurants) {
        try {
            // Restaurant 엔티티를 DTO로 변환
            List<RestaurantDto> restaurantDtos = restaurants.stream()
                    .map(r -> RestaurantDto.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .category(r.getCategory())
                            .locationText(r.getLocationText())
                            .description(r.getDescription())
                            .build())
                    .collect(Collectors.toList());

            // 추천 메시지 생성
            SuggestionDto suggestionDto = SuggestionDto.builder()
                    .message("맛집을 추천해드릴게요!")
                    .restaurants(restaurantDtos)
                    .targetUserId(userId)
                    .build();

            // 해당 사용자의 sessionId 조회
            String sessionId = sessionInterceptor.getSessionIdByUserId(userId);

            if (sessionId != null) {
                // Private 메시지 전송
                // "/user/queue/suggestions" 경로로 전송하면, 클라이언트는 "/user/queue/suggestions"를 구독해야 함
                messagingTemplate.convertAndSendToUser(
                        sessionId,
                        "/queue/suggestions",
                        suggestionDto
                );

                log.info("Private suggestion sent - userId: {}, sessionId: {}", userId, sessionId);
            } else {
                log.warn("Session not found for userId: {}", userId);
            }

        } catch (Exception e) {
            log.error("Failed to send suggestion to user: {}", userId, e);
        }
    }

    /**
     * 메시지에서 키워드 추출 (임시 구현)
     * - 실제로는 LLM이 분석하여 키워드를 추출해야 함
     */
    private String extractKeyword(String content) {
        if (content.contains("판교")) return "판교";
        if (content.contains("잠실")) return "잠실";
        if (content.contains("합정")) return "합정";
        return null;
    }
}
