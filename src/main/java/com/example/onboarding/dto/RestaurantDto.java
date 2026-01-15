package com.example.onboarding.dto;

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
     * 음식 카테고리
     */
    private String category;

    /**
     * 위치 정보
     */
    private String locationText;

    /**
     * 맛집 설명
     */
    private String description;
}
