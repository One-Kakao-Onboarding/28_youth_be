package com.example.onboarding.service;

import com.example.onboarding.dto.ClaudeAnalysisResult;
import com.example.onboarding.dto.RestaurantDto;
import com.example.onboarding.dto.SuggestionDto;
import com.example.onboarding.entity.ChatMessage;
import com.example.onboarding.entity.Restaurant;
import com.example.onboarding.repository.ChatMessageRepository;
import com.example.onboarding.repository.RestaurantRepository;
import com.example.onboarding.websocket.UserSessionChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 맛집 추천 서비스
 * - Claude AI 분석 및 맛집 추천 (비동기 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final RestaurantRepository restaurantRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionChannelInterceptor sessionInterceptor;
    private final ClaudeService claudeService;
    private final ChatMessageRepository chatMessageRepository;

    private static final int CONTEXT_MESSAGE_LIMIT = 10;
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    /**
     * 메시지 분석 및 맛집 추천 (비동기)
     * - Claude AI로 메시지를 분석하여 맛집 추천 필요 여부 판단
     * - 추천 결과를 해당 사용자에게만 Private으로 전송
     *
     * @param message 분석할 메시지
     */
    @Async
    public void analyzeMessageAndSuggest(ChatMessage message) {
        try {
            log.info("Starting Claude analysis for message: {}", message.getId());

            // 1. 대화 컨텍스트 가져오기 (최근 10개 메시지)
            List<String> conversationContext = fetchConversationContext(
                    message.getRoomId(),
                    message.getId()
            );

            // 2. Claude API로 대화 분석
            ClaudeAnalysisResult analysis = claudeService.analyzeConversation(
                    conversationContext,
                    message.getContent()
            );

            log.info("Claude analysis result - shouldRecommend: {}, confidence: {}, location: {}",
                    analysis.isShouldRecommend(),
                    analysis.getConfidence(),
                    analysis.getLocation());

            // 3. 추천 필요 여부 확인
            if (!analysis.isShouldRecommend()) {
                log.info("No recommendation needed for message: {}", message.getId());
                return;
            }

            if (analysis.getConfidence() < CONFIDENCE_THRESHOLD) {
                log.info("Confidence too low ({}) for message: {}",
                        analysis.getConfidence(),
                        message.getId());
                return;
            }

            // 4. 맛집 검색 (수정된 전략)
            RestaurantSearchResult searchResult = searchRestaurants(analysis, message.getSenderId().toString());

            if (searchResult.isEmpty()) {
                log.info("No restaurants found for analysis: {}", analysis);
                return;
            }

            // 5. 사용자에게 추천 전송
            sendSuggestionToUser(
                    message.getSenderId().toString(),
                    searchResult,
                    analysis
            );

            log.info("Suggestion sent to user: {}, count: {}",
                    message.getSenderId(),
                    searchResult.getTotalCount());

        } catch (Exception e) {
            log.error("Failed to analyze message and suggest restaurants", e);
        }
    }

    /**
     * 대화 컨텍스트 가져오기
     */
    private List<String> fetchConversationContext(Long roomId, Long currentMessageId) {
        try {
            List<ChatMessage> recentMessages = chatMessageRepository
                    .findTop10ByRoomIdAndIdLessThanOrderByIdDesc(
                            roomId,
                            currentMessageId
                    );

            return recentMessages.stream()
                    .map(msg -> msg.getSenderNickname() + ": " + msg.getContent())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to fetch conversation context", e);
            return Collections.emptyList();
        }
    }

    /**
     * 맛집 검색 (수정된 전략)
     * 전략 1: 지역 기반 즐겨찾기 및 상황 기반
     * 전략 2: 지역 및 상황 기반 랜덤 인기 맛집
     */
    private RestaurantSearchResult searchRestaurants(ClaudeAnalysisResult analysis, String userId) {
        String location = analysis.getLocation();
        List<String> categories = analysis.getCategories();

        // 전략 1: 즐겨찾기 맛집 (현재는 나중에 구현 예정이므로 빈 리스트)
        List<Restaurant> favoriteRestaurants = Collections.emptyList();
        // TODO: 즐겨찾기 기능 구현 시
        // favoriteRestaurants = favoriteRepository.findByUserIdAndLocationAndCategories(userId, location, categories);

        // 전략 2: 지역 및 상황 기반 랜덤 인기 맛집
        List<Restaurant> aiRecommendedRestaurants = searchByLocationAndCategories(location, categories);

        return new RestaurantSearchResult(favoriteRestaurants, aiRecommendedRestaurants);
    }

    /**
     * 지역 및 카테고리 기반 검색
     */
    private List<Restaurant> searchByLocationAndCategories(String location, List<String> categories) {
        List<Restaurant> restaurants;

        // 지역 기반 검색
        if (location != null && !location.isEmpty()) {
            restaurants = restaurantRepository.findByKeywordsContaining(location);

            // 카테고리로 필터링
            if (categories != null && !categories.isEmpty()) {
                restaurants = filterByCategories(restaurants, categories);
            }

            if (!restaurants.isEmpty()) {
                return restaurants.stream()
                        .limit(5)
                        .collect(Collectors.toList());
            }
        }

        // Fallback: 카테고리만으로 검색
        if (categories != null && !categories.isEmpty()) {
            for (String category : categories) {
                restaurants = restaurantRepository.findByKeywordsContaining(category);
                if (!restaurants.isEmpty()) {
                    return restaurants.stream()
                            .limit(5)
                            .collect(Collectors.toList());
                }
            }
        }

        // Last resort: 랜덤 인기 맛집
        return restaurantRepository.findAll().stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리로 맛집 필터링
     */
    private List<Restaurant> filterByCategories(List<Restaurant> restaurants, List<String> categories) {
        return restaurants.stream()
                .filter(r -> categories.stream()
                        .anyMatch(cat -> r.getKeywords() != null && r.getKeywords().contains(cat)))
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자에게 맛집 추천 전송 (카드 형식)
     */
    private void sendSuggestionToUser(
            String userId,
            RestaurantSearchResult searchResult,
            ClaudeAnalysisResult analysis) {
        try {
            // 카드 제목 생성
            String cardTitle = buildCardTitle(analysis);
            String cardImage = "/images/restaurant-map.jpg";  // 기본 지도 이미지

            // 즐겨찾기 맛집 DTO 변환
            List<RestaurantDto> favoriteDtos = convertToDto(searchResult.getFavoriteRestaurants());

            // AI 추천 맛집 DTO 변환
            List<RestaurantDto> aiRecommendedDtos = convertToDto(searchResult.getAiRecommendedRestaurants());

            // 카드 데이터 생성 (현재는 AI 추천만 표시, 즐겨찾기는 추후 추가)
            SuggestionDto.CardData cardData = SuggestionDto.CardData.builder()
                    .title(cardTitle)
                    .image(cardImage)
                    .restaurants(aiRecommendedDtos)  // 현재는 AI 추천만
                    .build();

            // Suggestion DTO 생성
            SuggestionDto suggestionDto = SuggestionDto.builder()
                    .type("card")
                    .message("맛집을 추천해드릴게요!")
                    .cardData(cardData)
                    .targetUserId(userId)
                    .time(LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)))
                    .build();

            // 해당 사용자의 sessionId 조회
            String sessionId = sessionInterceptor.getSessionIdByUserId(userId);

            if (sessionId != null) {
                // Private 메시지 전송
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
     * Restaurant 엔티티를 DTO로 변환
     */
    private List<RestaurantDto> convertToDto(List<Restaurant> restaurants) {
        return restaurants.stream()
                .map(r -> RestaurantDto.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .category(r.getCategory())
                        .locationText(r.getLocationText())
                        .address(r.getLocationText())  // address = locationText
                        .description(r.getDescription())
                        .rating(r.getRating() != null ? r.getRating() : 4.5)
                        .image(r.getImageUrl() != null ? r.getImageUrl() : "/images/placeholder-restaurant.jpg")
                        .distance(r.getDistanceText() != null ? r.getDistanceText() : "거리 정보 없음")
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 카드 제목 생성
     */
    private String buildCardTitle(ClaudeAnalysisResult analysis) {
        StringBuilder title = new StringBuilder();

        if (analysis.getLocation() != null && !analysis.getLocation().isEmpty()) {
            title.append(analysis.getLocation()).append(" ");
        }

        if (analysis.getCategories() != null && !analysis.getCategories().isEmpty()) {
            title.append(analysis.getCategories().get(0)).append(" ");
        }

        title.append("맛집 추천 리스트");

        return title.toString();
    }

    /**
     * 맛집 검색 결과를 담는 내부 클래스
     */
    @lombok.Getter
    private static class RestaurantSearchResult {
        private final List<Restaurant> favoriteRestaurants;
        private final List<Restaurant> aiRecommendedRestaurants;

        public RestaurantSearchResult(List<Restaurant> favoriteRestaurants,
                                     List<Restaurant> aiRecommendedRestaurants) {
            this.favoriteRestaurants = favoriteRestaurants != null ? favoriteRestaurants : Collections.emptyList();
            this.aiRecommendedRestaurants = aiRecommendedRestaurants != null ? aiRecommendedRestaurants : Collections.emptyList();
        }

        public boolean isEmpty() {
            return favoriteRestaurants.isEmpty() && aiRecommendedRestaurants.isEmpty();
        }

        public int getTotalCount() {
            return favoriteRestaurants.size() + aiRecommendedRestaurants.size();
        }
    }
}
