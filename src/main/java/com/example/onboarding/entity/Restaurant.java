package com.example.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 맛집 정보 엔티티
 * - LLM이 추천할 맛집 더미 데이터를 저장
 */
@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    /**
     * 맛집 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 맛집 이름
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 음식 카테고리 (예: 한식, 일식, 중식 등)
     */
    @Column(length = 50)
    private String category;

    /**
     * 위치 정보 (텍스트)
     */
    @Column(name = "location_text", length = 200)
    private String locationText;

    /**
     * 맛집 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 검색 키워드 (쉼표로 구분된 문자열)
     * 예: "판교,삼겹살,회식"
     */
    @Column(columnDefinition = "TEXT")
    private String keywords;
}
