package com.example.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 사용자 엔티티
 * - 별도 인증 없이 클라이언트에서 생성한 UUID와 닉네임을 저장
 * - DB에는 저장하되, 인증 체크는 하지 않음
 */
@Entity
@Table(name = "chat_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatUser {

    /**
     * 사용자 고유 ID (UUID)
     * 클라이언트에서 생성한 UUID를 그대로 사용
     */
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    /**
     * 사용자 닉네임
     */
    @Column(nullable = false, length = 50)
    private String nickname;

    /**
     * 계정 생성 시각
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
