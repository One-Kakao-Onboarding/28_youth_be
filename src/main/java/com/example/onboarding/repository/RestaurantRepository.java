package com.example.onboarding.repository;

import com.example.onboarding.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 맛집 Repository
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /**
     * 키워드로 맛집 검색 (keywords 필드에서 검색)
     * @param keyword 검색 키워드
     * @return 맛집 리스트
     */
    @Query("SELECT r FROM Restaurant r WHERE r.keywords LIKE %:keyword%")
    List<Restaurant> findByKeywordsContaining(@Param("keyword") String keyword);
}
