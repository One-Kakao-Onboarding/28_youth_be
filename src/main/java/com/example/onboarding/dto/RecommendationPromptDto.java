package com.example.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * 맛집 추천 가능 알림 DTO
 * - 서버가 Claude 분석 후 "추천이 가능합니다" 알림을 클라이언트에게 전송
 * - 클라이언트는 이를 보고 추천을 요청할지 선택
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RecommendationPromptDto {

    /**
     * 메시지 타입 (항상 "recommendation-prompt")
     */
    @JsonProperty("type")
    @Builder.Default
    private String type = "recommendation-prompt";

    /**
     * 알림 메시지
     */
    @JsonProperty("message")
    private String message;

    /**
     * 분석 결과 식별자 (클라이언트가 추천 요청 시 사용)
     */
    @JsonProperty("analysisId")
    private String analysisId;

    /**
     * 추출된 지역 (예: "판교", "강남")
     */
    @JsonProperty("location")
    private String location;

    /**
     * 식사 종류 (예: "점심", "저녁")
     */
    @JsonProperty("mealType")
    private String mealType;

    /**
     * 신뢰도 (0.0 ~ 1.0)
     */
    @JsonProperty("confidence")
    private Double confidence;

    /**
     * 시간 정보 (예: "오후 2:30")
     */
    @JsonProperty("time")
    private String time;
}
