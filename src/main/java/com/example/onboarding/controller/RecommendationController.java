package com.example.onboarding.controller;

import com.example.onboarding.dto.RecommendationRequestDto;
import com.example.onboarding.service.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 맛집 추천 REST API 컨트롤러
 * - 클라이언트가 HTTP POST 요청으로 맛집 추천을 요청
 * - 응답은 WebSocket(/sub/room/1)으로 브로드캐스트
 */
@Tag(name = "Recommendation", description = "맛집 추천 요청 API")
@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final SuggestionService suggestionService;

    /**
     * 맛집 추천 요청
     * - 클라이언트가 서버로부터 받은 analysisId를 사용하여 맛집 추천을 요청
     * - 실제 추천 결과는 WebSocket(/sub/room/1)으로 전송됨
     *
     * @param requestDto 추천 요청 DTO (analysisId, userId 포함)
     * @return 요청 수락 응답
     */
    @Operation(
            summary = "맛집 추천 요청",
            description = "서버가 보낸 analysisId를 사용하여 맛집 추천을 요청합니다. " +
                    "실제 추천 결과는 WebSocket(/sub/room/1)으로 브로드캐스트됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 요청이 정상적으로 처리됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (analysisId 또는 userId 누락)"),
            @ApiResponse(responseCode = "404", description = "analysisId를 찾을 수 없음 (만료되었거나 잘못된 ID)")
    })
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestRecommendation(
            @RequestBody RecommendationRequestDto requestDto) {

        log.info("REST API - Recommendation request received - analysisId: {}, userId: {}",
                requestDto.getAnalysisId(), requestDto.getUserId());

        // 필수 파라미터 검증
        if (requestDto.getAnalysisId() == null || requestDto.getAnalysisId().isBlank()) {
            log.warn("Missing analysisId in request");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "analysisId is required"));
        }

        if (requestDto.getUserId() == null || requestDto.getUserId().isBlank()) {
            log.warn("Missing userId in request");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId is required"));
        }

        // 추천 요청 처리 (비동기)
        suggestionService.provideRecommendation(
                requestDto.getAnalysisId(),
                requestDto.getUserId()
        );

        // 즉시 응답 반환 (실제 추천은 WebSocket으로 전송됨)
        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "message", "추천 요청이 접수되었습니다. 곧 WebSocket으로 결과를 받게 됩니다."
        ));
    }
}
