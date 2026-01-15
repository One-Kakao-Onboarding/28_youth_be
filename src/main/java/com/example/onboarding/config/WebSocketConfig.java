package com.example.onboarding.config;

import com.example.onboarding.websocket.UserHandshakeInterceptor;
import com.example.onboarding.websocket.UserSessionChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정
 * - STOMP 프로토콜을 사용한 채팅 서버 설정
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserSessionChannelInterceptor userSessionChannelInterceptor;
    private final UserHandshakeInterceptor userHandshakeInterceptor;

    /**
     * 메시지 브로커 설정
     * - /sub: 클라이언트가 구독하는 prefix (브로드캐스트용)
     * - /user: 클라이언트가 구독하는 prefix (개인 메시지용)
     * - /pub: 클라이언트가 메시지를 발행하는 prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple In-Memory Broker 활성화 (실시간 메시지 브로드캐스팅)
        // /sub: 채팅방 메시지 등 브로드캐스트
        // /user: 개인별 추천 알림, 맛집 추천, 에러 메시지
        config.enableSimpleBroker("/sub", "/user");

        // 클라이언트가 서버로 메시지 전송 시 사용하는 prefix
        config.setApplicationDestinationPrefixes("/pub");

        // User-specific 메시지를 위한 prefix 설정
        config.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트 등록
     * - /ws-chat: WebSocket 연결 엔드포인트
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .addInterceptors(userHandshakeInterceptor)  // Handshake 시 사용자 정보 추출
                .setAllowedOriginPatterns("*")  // CORS 설정
                .withSockJS();  // SockJS fallback 지원
    }

    /**
     * 클라이언트 인바운드 채널 설정
     * - 사용자 세션 정보를 저장하는 ChannelInterceptor 등록
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(userSessionChannelInterceptor);
    }
}
