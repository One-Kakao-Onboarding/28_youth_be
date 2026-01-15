package com.example.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 메시지 엔티티
 * - 채팅방에서 주고받는 모든 메시지를 저장
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_room_created", columnList = "room_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    /**
     * 메시지 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 메시지가 속한 채팅방 ID
     */
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    /**
     * 메시지 발신자 ID (UUID)
     */
    @Column(name = "sender_id", columnDefinition = "UUID", nullable = false)
    private UUID senderId;

    /**
     * 발신자 닉네임 (비정규화 - 조회 성능 향상)
     */
    @Column(name = "sender_nickname", length = 50)
    private String senderNickname;

    /**
     * 메시지 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 메시지 타입 (TALK, ENTER, SUGGEST)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    /**
     * 메시지 생성 시각
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
