package com.example.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Claude API 분석 결과 DTO
 * - 대화 내용을 분석한 결과를 구조화
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaudeAnalysisResult {

    /**
     * 맛집 추천이 필요한지 여부
     */
    @JsonProperty("shouldRecommend")
    private boolean shouldRecommend;

    /**
     * 추출된 지역 (예: "판교", "강남", "잠실")
     */
    @JsonProperty("location")
    private String location;

    /**
     * 식사 종류 (예: "점심", "저녁", "브런치")
     */
    @JsonProperty("mealType")
    private String mealType;

    /**
     * 음식 카테고리 선호도 (예: ["한식", "일식"])
     */
    @JsonProperty("categories")
    private List<String> categories;

    /**
     * 추가 선호사항 (예: ["회식", "데이트", "분위기 좋은"])
     */
    @JsonProperty("preferences")
    private List<String> preferences;

    /**
     * 신뢰도 (0.0 ~ 1.0)
     */
    @JsonProperty("confidence")
    private double confidence;

    /**
     * 추천 결정 이유
     */
    @JsonProperty("reasoning")
    private String reasoning;
}
