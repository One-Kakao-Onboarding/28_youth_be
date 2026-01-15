package com.example.onboarding.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * - @Async 어노테이션을 사용하기 위한 설정
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
