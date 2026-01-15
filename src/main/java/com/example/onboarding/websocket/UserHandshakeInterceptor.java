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
 * - WebSocket 연결 시 기본 핸드셰이크 처리
 * - 사용자 정보는 STOMP CONNECT 시 헤더에서 추출됨
 */
@Slf4j
@Component
public class UserHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * Handshake 전 실행
     * - WebSocket 연결 허용 (사용자 정보는 STOMP CONNECT 시 검증)
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        log.info("WebSocket Handshake - Connection allowed");
        return true;  // 모든 연결 허용 (STOMP CONNECT 시 사용자 정보 검증)
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Handshake 후 처리 (필요 시 구현)
    }
}
