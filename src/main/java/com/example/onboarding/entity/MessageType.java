package com.example.onboarding.entity;

/**
 * 채팅 메시지 타입
 * - TALK: 일반 대화 메시지
 * - ENTER: 사용자 입장 메시지
 * - SUGGEST: 맛집 추천 메시지 (LLM이 트리거한 메시지)
 */
public enum MessageType {
    TALK,
    ENTER,
    SUGGEST
}
