package com.example.onboarding.repository;

import com.example.onboarding.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 채팅 메시지 Repository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지 조회 (최신순)
     * @param roomId 채팅방 ID
     * @param pageable 페이징 정보
     * @return 메시지 리스트
     */
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    /**
     * 특정 메시지 이전의 최근 메시지 조회 (대화 컨텍스트용)
     * @param roomId 채팅방 ID
     * @param messageId 기준 메시지 ID
     * @return 메시지 리스트 (최대 10개)
     */
    List<ChatMessage> findTop10ByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long messageId);
}
