package com.example.onboarding.service;

import com.example.onboarding.dto.ClaudeAnalysisResult;
import com.example.onboarding.dto.RecommendationPromptDto;
import com.example.onboarding.dto.RestaurantDto;
import com.example.onboarding.dto.SuggestionDto;
import com.example.onboarding.entity.ChatMessage;
import com.example.onboarding.entity.Restaurant;
import com.example.onboarding.repository.ChatMessageRepository;
import com.example.onboarding.repository.RestaurantRepository;
import com.example.onboarding.websocket.UserSessionChannelInterceptor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 맛집 추천 서비스
 * - Claude AI 분석 및 맛집 추천 (2단계 프로세스)
 * - 1단계: 분석 후 추천 가능 알림
 * - 2단계: 사용자 요청 시 실제 추천 제공
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
    private static final int ANALYSIS_CACHE_EXPIRE_MINUTES = 5;
    private static final Long DEFAULT_ROOM_ID = 1L;  // 기본 채팅방 ID

    /**
     * 분석 결과 임시 저장소 (인메모리 캐시)
     * Key: analysisId (UUID)
     * Value: AnalysisCacheEntry
     */
    private final ConcurrentHashMap<String, AnalysisCacheEntry> analysisCache = new ConcurrentHashMap<>();

    /**
     * 1단계: 메시지 분석 (비동기)
     * - Claude AI로 메시지를 분석하여 맛집 추천 필요 여부 판단
     * - shouldRecommend=true이면 분석 결과를 캐시에 저장하고 프롬프트 전송
     * - 실제 맛집 검색은 하지 않음 (사용자 요청 대기)
     *
     * @param message 분석할 메시지
     */
    @Async
    public void analyzeMessage(ChatMessage message) {
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

            // 4. 분석 결과를 캐시에 저장
            String analysisId = UUID.randomUUID().toString();
            AnalysisCacheEntry cacheEntry = new AnalysisCacheEntry();
            cacheEntry.setAnalysis(analysis);
            cacheEntry.setUserId(message.getSenderId().toString());
            cacheEntry.setCreatedAt(LocalDateTime.now());
            cacheEntry.setMessageId(message.getId());
            cacheEntry.setProcessed(false);

            analysisCache.put(analysisId, cacheEntry);

            log.info("Analysis cached - analysisId: {}, userId: {}", analysisId, message.getSenderId());

            // 5. 사용자에게 추천 가능 알림 전송
            sendRecommendationPrompt(
                    message.getSenderId().toString(),
                    analysisId,
                    analysis
            );

            log.info("Recommendation prompt sent to user: {}", message.getSenderId());

        } catch (Exception e) {
            log.error("Failed to analyze message", e);
        }
    }

    /**
     * 2단계: 맛집 추천 제공
     * - 클라이언트가 추천을 요청하면 캐시에서 분석 결과를 가져와 맛집 검색 및 추천
     *
     * @param analysisId 분석 결과 식별자
     * @param userId 요청 사용자 ID
     */
    public void provideRecommendation(String analysisId, String userId) {
        try {
            log.info("Recommendation requested - analysisId: {}, userId: {}", analysisId, userId);

            // 1. 캐시에서 분석 결과 가져오기
            AnalysisCacheEntry cacheEntry = analysisCache.get(analysisId);

            if (cacheEntry == null) {
                log.warn("Analysis not found in cache - analysisId: {}", analysisId);
                sendErrorMessage(userId, "추천 요청이 만료되었습니다. 다시 시도해주세요.");
                return;
            }

            // 2. 이미 처리된 요청인지 확인
            if (cacheEntry.isProcessed()) {
                log.warn("Analysis already processed - analysisId: {}", analysisId);
                sendErrorMessage(userId, "이미 처리된 요청입니다.");
                return;
            }

            // 3. 사용자 ID 검증
            if (!cacheEntry.getUserId().equals(userId)) {
                log.warn("User ID mismatch - expected: {}, actual: {}", cacheEntry.getUserId(), userId);
                sendErrorMessage(userId, "잘못된 요청입니다.");
                return;
            }

            // 4. 처리 완료 표시
            cacheEntry.setProcessed(true);

            // 5. 맛집 검색
            ClaudeAnalysisResult analysis = cacheEntry.getAnalysis();
            RestaurantSearchResult searchResult = searchRestaurants(analysis, userId);

            if (searchResult.isEmpty()) {
                log.info("No restaurants found for analysis: {}", analysis);
                sendErrorMessage(userId, "추천 가능한 맛집을 찾지 못했습니다.");
                // 캐시에서 제거
                analysisCache.remove(analysisId);
                return;
            }

            // 6. 사용자에게 추천 전송
            sendSuggestionToUser(userId, searchResult, analysis);

            log.info("Recommendation provided - analysisId: {}, userId: {}, count: {}",
                    analysisId, userId, searchResult.getTotalCount());

            // 7. 캐시에서 제거 (사용 완료)
            analysisCache.remove(analysisId);

        } catch (Exception e) {
            log.error("Failed to provide recommendation", e);
            sendErrorMessage(userId, "추천 처리 중 오류가 발생했습니다.");
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
                    .targetUserId(userId)  // 클라이언트가 자신의 추천만 처리하도록
                    .time(LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)))
                    .build();

            // /sub/room/1로 브로드캐스트
            String destination = "/sub/room/" + DEFAULT_ROOM_ID;
            messagingTemplate.convertAndSend(destination, suggestionDto);

            log.info("Suggestion broadcast - userId: {}, destination: {}", userId, destination);

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
     * 추천 가능 알림 전송
     */
    private void sendRecommendationPrompt(String userId, String analysisId, ClaudeAnalysisResult analysis) {
        try {
            RecommendationPromptDto promptDto = RecommendationPromptDto.builder()
                    .type("recommendation-prompt")
                    .message("맛집 추천이 가능합니다")
                    .analysisId(analysisId)
                    .location(analysis.getLocation())
                    .mealType(analysis.getMealType())
                    .confidence(analysis.getConfidence())
                    .time(LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)))
                    .build();

            log.info("Sending recommendation prompt - userId: {}, analysisId: {}", userId, analysisId);
            log.info("Prompt DTO: {}", promptDto);

            // /sub/room/1로 브로드캐스트 (채팅 메시지와 동일한 경로)
            String destination = "/sub/room/" + DEFAULT_ROOM_ID;
            messagingTemplate.convertAndSend(destination, promptDto);

            log.info("Recommendation prompt broadcast - destination: {}, analysisId: {}", destination, analysisId);

        } catch (Exception e) {
            log.error("Failed to send recommendation prompt to user: {}", userId, e);
        }
    }

    /**
     * 에러 메시지 전송
     */
    private void sendErrorMessage(String userId, String errorMessage) {
        try {
            Map<String, String> error = Map.of(
                    "type", "error",
                    "message", errorMessage,
                    "userId", userId,  // 클라이언트가 자신의 에러만 처리하도록
                    "time", LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN))
            );

            // /sub/room/1로 브로드캐스트
            String destination = "/sub/room/" + DEFAULT_ROOM_ID;
            messagingTemplate.convertAndSend(destination, error);

            log.info("Error message broadcast - userId: {}, message: {}", userId, errorMessage);

        } catch (Exception e) {
            log.error("Failed to send error message to user: {}", userId, e);
        }
    }

    /**
     * 만료된 분석 결과 정리 스케줄러
     * - 매 1분마다 실행
     * - 5분 이상 경과한 분석 결과 삭제
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredAnalysis() {
        try {
            LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(ANALYSIS_CACHE_EXPIRE_MINUTES);
            int removedCount = 0;

            Iterator<Map.Entry<String, AnalysisCacheEntry>> iterator = analysisCache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, AnalysisCacheEntry> entry = iterator.next();
                if (entry.getValue().getCreatedAt().isBefore(expiryTime)) {
                    iterator.remove();
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                log.info("Cleaned up {} expired analysis results", removedCount);
            }

        } catch (Exception e) {
            log.error("Failed to cleanup expired analysis", e);
        }
    }

    /**
     * 분석 결과 캐시 엔트리 내부 클래스
     */
    @Data
    private static class AnalysisCacheEntry {
        private ClaudeAnalysisResult analysis;
        private String userId;
        private LocalDateTime createdAt;
        private Long messageId;
        private boolean processed;
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
