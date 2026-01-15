package com.example.onboarding.dto;

import com.example.onboarding.entity.MessageType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 DTO
 * - WebSocket을 통해 송수신되는 메시지 형식
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    /**
     * 메시지 ID (응답 시에만 포함)
     */
    private Long id;

    /**
     * 채팅방 ID
     */
    private Long roomId;

    /**
     * 발신자 ID (UUID 문자열)
     */
    private String senderId;

    /**
     * 발신자 닉네임
     */
    private String senderNickname;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 메시지 타입 (TALK, ENTER, SUGGEST)
     */
    private MessageType type;

    /**
     * 메시지 생성 시각 (응답 시에만 포함)
     */
    private LocalDateTime createdAt;
}
