package com.example.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * 맛집 추천 요청 DTO
 * - 클라이언트가 서버에게 "맛집을 추천해주세요" 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequestDto {

    /**
     * 분석 결과 식별자
     * - 서버가 전송한 RecommendationPromptDto의 analysisId
     */
    @JsonProperty("analysisId")
    private String analysisId;

    /**
     * 요청 사용자 ID
     */
    @JsonProperty("userId")
    private String userId;
}
