package com.example.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * 맛집 정보 DTO
 * - LLM이 추천하는 맛집 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantDto {

    /**
     * 맛집 ID
     */
    private Long id;

    /**
     * 맛집 이름
     */
    private String name;

    /**
     * 음식 카테고리 (예: "한식 • 백반")
     */
    private String category;

    /**
     * 위치 정보 (백엔드용)
     */
    private String locationText;

    /**
     * 맛집 설명
     */
    private String description;

    /**
     * 평점 (0.0 ~ 5.0)
     */
    @JsonProperty("rating")
    private Double rating;

    /**
     * 주소 (프론트엔드용, locationText와 동일)
     */
    @JsonProperty("address")
    private String address;

    /**
     * 이미지 URL
     */
    @JsonProperty("image")
    private String image;

    /**
     * 거리 정보 (예: "도보 5분")
     */
    @JsonProperty("distance")
    private String distance;
}
