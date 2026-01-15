package com.example.onboarding.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Onboarding Hackathon API")
                        .version("1.0")
                        .description(
                                "<p>이 문서는 Onboarding Hackathon 프로젝트의 RESTful API 명세입니다.</p>" +
                                "<p><b>WebSocket 채팅 API (STOMP)는 이 문서에 포함되어 있지 않습니다.</b> " +
                                "자세한 내용은 별도로 제공된 <a href=\"#\" target=\"_blank\">API 명세서 (Markdown)</a> 문서를 참조해 주세요.</p>"+
                                "<p>WebSocket Connect Endpoint: <code>/ws</code></p>" +
                                "<p>메시지 발행 (클라이언트 → 서버) Destination: <code>/pub/message</code></p>" +
                                "<p>메시지 구독 (서버 → 클라이언트) Destination: <code>/sub/chat/room/{roomId}</code></p>"
                        )
                        .contact(new Contact().name("Your Name").email("your.email@example.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org"))
                );
    }
}
