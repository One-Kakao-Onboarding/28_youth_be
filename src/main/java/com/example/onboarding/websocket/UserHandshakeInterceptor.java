package com.example.onboarding.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Handshake 인터셉터
 * - WebSocket 연결 시 HTTP 헤더에서 사용자 정보를 추출하여 세션에 저장
 */
@Slf4j
@Component
public class UserHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * Handshake 전 실행
     * - HTTP 헤더에서 userId와 nickname을 추출하여 WebSocket 세션 속성에 저장
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // HTTP 헤더에서 사용자 정보 추출
        String userId = request.getHeaders().getFirst("X-User-Id");
        String nickname = request.getHeaders().getFirst("X-Nickname");

        if (userId != null && nickname != null) {
            // WebSocket 세션 속성에 저장
            attributes.put("userId", userId);
            attributes.put("nickname", nickname);

            log.info("WebSocket Handshake - userId: {}, nickname: {}", userId, nickname);
            return true;
        }

        log.warn("WebSocket Handshake failed - Missing userId or nickname in headers");
        return false;  // 사용자 정보가 없으면 연결 거부
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Handshake 후 처리 (필요 시 구현)
    }
}
