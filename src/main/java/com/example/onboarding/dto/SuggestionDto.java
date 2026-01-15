package com.example.onboarding.dto;

import lombok.*;

import java.util.List;

/**
 * 맛집 추천 응답 DTO
 * - LLM 분석 결과로 특정 사용자에게 전송되는 맛집 리스트
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionDto {

    /**
     * 추천 메시지
     */
    private String message;

    /**
     * 추천 맛집 리스트
     */
    private List<RestaurantDto> restaurants;

    /**
     * 추천 대상 사용자 ID
     */
    private String targetUserId;
}
