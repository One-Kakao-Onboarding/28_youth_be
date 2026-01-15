package com.example.onboarding.repository;

import com.example.onboarding.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 채팅방 Repository
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
