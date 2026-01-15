package com.example.onboarding.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP 채널 인터셉터
 * - STOMP 메시지 처리 시 사용자 세션 정보를 관리
 * - CONNECT 시 세션에 사용자 정보 저장
 * - DISCONNECT 시 세션 정보 제거
 */
@Slf4j
@Component
public class UserSessionChannelInterceptor implements ChannelInterceptor {

    // sessionId -> userId 매핑 (Private 메시지 전송 시 사용)
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * 메시지 전송 전 실행
     * - CONNECT: 세션에 사용자 정보 저장
     * - DISCONNECT: 세션 정보 제거
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            // CONNECT 명령 처리
            if (StompCommand.CONNECT.equals(command)) {
                // STOMP CONNECT 헤더에서 사용자 정보 추출
                String userId = accessor.getFirstNativeHeader("X-User-Id");
                String nickname = accessor.getFirstNativeHeader("X-Nickname");

                if (userId != null && nickname != null) {
                    String sessionId = accessor.getSessionId();

                    // 세션 속성에 사용자 정보 저장
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null) {
                        sessionAttributes.put("userId", userId);
                        sessionAttributes.put("nickname", nickname);
                    }

                    // 세션-사용자 매핑 저장
                    sessionUserMap.put(sessionId, userId);

                    log.info("STOMP CONNECT - sessionId: {}, userId: {}, nickname: {}",
                            sessionId, userId, nickname);
                } else {
                    log.warn("STOMP CONNECT failed - Missing X-User-Id or X-Nickname in STOMP headers");
                }
            }
            // DISCONNECT 명령 처리
            else if (StompCommand.DISCONNECT.equals(command)) {
                String sessionId = accessor.getSessionId();
                String userId = sessionUserMap.remove(sessionId);

                log.info("STOMP DISCONNECT - sessionId: {}, userId: {}", sessionId, userId);
            }
        }

        return message;
    }

    /**
     * sessionId로 userId 조회
     */
    public String getUserIdBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    /**
     * userId로 sessionId 조회
     */
    public String getSessionIdByUserId(String userId) {
        return sessionUserMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(userId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
