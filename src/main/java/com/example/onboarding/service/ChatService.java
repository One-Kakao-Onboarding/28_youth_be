package com.example.onboarding.service;

import com.example.onboarding.dto.ChatMessageDto;
import com.example.onboarding.entity.ChatMessage;
import com.example.onboarding.entity.ChatUser;
import com.example.onboarding.entity.MessageType;
import com.example.onboarding.repository.ChatMessageRepository;
import com.example.onboarding.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 채팅 서비스
 * - 채팅 메시지 처리 및 브로드캐스트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatUserRepository chatUserRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SuggestionService suggestionService;

    /**
     * 채팅 메시지 처리
     * - DB에 저장 후 해당 채팅방 구독자들에게 브로드캐스트
     * - TALK 타입 메시지인 경우 LLM 분석 트리거 (비동기)
     *
     * @param messageDto 클라이언트로부터 받은 메시지 DTO
     * @param senderId WebSocket 세션에서 추출한 발신자 ID
     * @param senderNickname WebSocket 세션에서 추출한 발신자 닉네임
     */
    @Transactional
    public void handleChatMessage(ChatMessageDto messageDto, String senderId, String senderNickname) {
        try {
            UUID senderUuid = UUID.fromString(senderId);

            // 사용자 정보 저장 또는 업데이트 (없으면 생성)
            chatUserRepository.findById(senderUuid)
                    .orElseGet(() -> {
                        ChatUser newUser = ChatUser.builder()
                                .id(senderUuid)
                                .nickname(senderNickname)
                                .build();
                        return chatUserRepository.save(newUser);
                    });

            // 메시지 엔티티 생성
            ChatMessage message = ChatMessage.builder()
                    .roomId(messageDto.getRoomId())
                    .senderId(senderUuid)
                    .senderNickname(senderNickname)
                    .content(messageDto.getContent())
                    .type(messageDto.getType() != null ? messageDto.getType() : MessageType.TALK)
                    .build();

            // DB에 저장
            ChatMessage savedMessage = chatMessageRepository.save(message);

            // 응답 DTO 생성
            ChatMessageDto responseDto = ChatMessageDto.builder()
                    .id(savedMessage.getId())
                    .roomId(savedMessage.getRoomId())
                    .senderId(savedMessage.getSenderId().toString())
                    .senderNickname(savedMessage.getSenderNickname())
                    .content(savedMessage.getContent())
                    .type(savedMessage.getType())
                    .createdAt(savedMessage.getCreatedAt())
                    .build();

            // 해당 채팅방 구독자들에게 브로드캐스트
            String destination = "/sub/room/" + savedMessage.getRoomId();
            messagingTemplate.convertAndSend(destination, responseDto);

            log.info("Message broadcasted - roomId: {}, senderId: {}, type: {}",
                    savedMessage.getRoomId(), senderId, savedMessage.getType());

            // TALK 타입 메시지인 경우 LLM 분석 트리거 (비동기)
            if (savedMessage.getType() == MessageType.TALK) {
                suggestionService.analyzeMessage(savedMessage);
            }

        } catch (Exception e) {
            log.error("Failed to handle chat message", e);
            throw new RuntimeException("메시지 처리 중 오류가 발생했습니다.", e);
        }
    }
}
