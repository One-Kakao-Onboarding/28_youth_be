package com.example.onboarding.controller;

import com.example.onboarding.dto.ChatMessageDto;
import com.example.onboarding.service.ChatService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket 채팅 컨트롤러
 * - STOMP 프로토콜을 통한 채팅 메시지 처리
 */
@Tag(name = "WebSocket Chat", description = "STOMP 프로토콜을 이용한 실시간 채팅 API (REST API가 아님). 상세 내용은 별도 문서를 참조하세요.")
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 채팅 메시지 수신 및 처리
     * - 클라이언트가 /pub/message로 전송한 메시지를 처리
     * - WebSocket 세션에서 사용자 정보를 추출하여 메시지 발신자 식별
     *
     * @param messageDto 클라이언트로부터 받은 메시지 DTO
     * @param headerAccessor WebSocket 세션 정보 접근자
     */
    @Hidden // WebSocket 엔드포인트는 REST API 문서에서 숨깁니다.
    @MessageMapping("/message")
    public void sendMessage(ChatMessageDto messageDto, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // WebSocket 세션 속성에서 사용자 정보 추출
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

            if (sessionAttributes == null) {
                log.error("Session attributes not found");
                return;
            }

            String senderId = (String) sessionAttributes.get("userId");
            String senderNickname = (String) sessionAttributes.get("nickname");

            if (senderId == null || senderNickname == null) {
                log.error("User information not found in session");
                return;
            }

            // 채팅 서비스로 메시지 처리 위임
            chatService.handleChatMessage(messageDto, senderId, senderNickname);

        } catch (Exception e) {
            log.error("Failed to process message", e);
        }
    }
}
