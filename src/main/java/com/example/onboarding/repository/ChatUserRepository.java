package com.example.onboarding.repository;

import com.example.onboarding.entity.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * 채팅 사용자 Repository
 */
@Repository
public interface ChatUserRepository extends JpaRepository<ChatUser, UUID> {
}
