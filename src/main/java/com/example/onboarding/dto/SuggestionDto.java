package com.example.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * 맛집 추천 응답 DTO
 * - LLM 분석 결과로 특정 사용자에게 전송되는 맛집 리스트
 * - 프론트엔드 Message 인터페이스와 호환
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionDto {

    /**
     * 메시지 타입 (항상 "card")
     */
    @JsonProperty("type")
    @Builder.Default
    private String type = "card";

    /**
     * 추천 메시지 텍스트
     */
    @JsonProperty("message")
    private String message;

    /**
     * 카드 데이터 (제목, 이미지, 맛집 리스트)
     */
    @JsonProperty("cardData")
    private CardData cardData;

    /**
     * 추천 대상 사용자 ID (프론트엔드로 전송 안함)
     */
    @JsonIgnore
    private String targetUserId;

    /**
     * 시간 정보 (예: "오후 2:30")
     */
    @JsonProperty("time")
    private String time;

    /**
     * 카드 데이터 내부 구조
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardData {

        /**
         * 카드 제목 (예: "강남 한식 맛집 추천 리스트")
         */
        @JsonProperty("title")
        private String title;

        /**
         * 카드 이미지 URL (지도 이미지 등)
         */
        @JsonProperty("image")
        private String image;

        /**
         * 추천 맛집 리스트
         */
        @JsonProperty("restaurants")
        private List<RestaurantDto> restaurants;
    }
}
