# Onboarding Hackathon - Backend

AI 기반 맛집 추천 채팅 애플리케이션의 백엔드 서버입니다. 실시간 채팅 기능과 Claude AI를 활용한 지능형 맛집 추천 시스템을 제공합니다.

## 주요 기능

### 1. 실시간 채팅
- WebSocket (STOMP 프로토콜) 기반 실시간 채팅
- 다중 사용자 채팅방 지원
- 채팅 메시지 히스토리 저장 (PostgreSQL)

### 2. AI 기반 맛집 추천
- Claude AI를 활용한 대화 맥락 분석
- 2단계 추천 프로세스:
  - **1단계**: 대화 분석 후 추천 가능 여부 알림
  - **2단계**: 사용자 요청 시 실제 맛집 추천 제공
- 지역, 음식 카테고리, 식사 유형 등 고려한 맞춤형 추천

### 3. API 문서화
- Swagger/OpenAPI 3.0 자동 문서화
- `/swagger-ui.html` 에서 API 명세 확인 가능

## 기술 스택

- **Framework**: Spring Boot 3.3.5
- **Language**: Java 21
- **Database**: PostgreSQL (Supabase)
- **AI Integration**: Claude API (Anthropic)
- **Real-time Communication**: WebSocket (STOMP)
- **Documentation**: SpringDoc OpenAPI
- **Build Tool**: Gradle

## 의존성

주요 라이브러리:
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter WebSocket
- Spring Boot Starter WebFlux (Claude API 통신)
- PostgreSQL Driver
- SpringDoc OpenAPI UI
- Lombok

## 환경 설정

### 필수 환경 변수

프로젝트 실행을 위해 다음 환경 변수를 설정해야 합니다:

```bash
# 데이터베이스 비밀번호
DB_PASSWORD=your_database_password

# Claude API 키
ANTHROPIC_API_KEY=your_anthropic_api_key
```

### application.yml 설정

주요 설정 항목:
- 데이터베이스: PostgreSQL (Supabase)
- 서버 포트: 8080
- Claude 모델: claude-3-haiku-20240307
- JPA: DDL auto-update, SQL 로깅 활성화

## 빌드 및 실행

### 사전 요구사항
- Java 21 이상
- Gradle 7.0 이상

### 실행 방법

1. 환경 변수 설정:
```bash
export DB_PASSWORD=your_database_password
export ANTHROPIC_API_KEY=your_anthropic_api_key
```

2. 애플리케이션 실행:
```bash
./gradlew bootRun
```

3. 빌드:
```bash
./gradlew build
```

4. JAR 파일 실행:
```bash
java -jar build/libs/onboarding-0.0.1-SNAPSHOT.jar
```

## API 엔드포인트

### REST API

#### Health Check
- `GET /health` - 서버 상태 확인

#### Chat Room
- `GET /api/chat-rooms` - 채팅방 목록 조회
- `POST /api/chat-rooms` - 새 채팅방 생성
- `GET /api/chat-rooms/{roomId}` - 특정 채팅방 정보 조회
- `GET /api/chat-rooms/{roomId}/messages` - 채팅 메시지 히스토리 조회

### WebSocket Endpoints

WebSocket 연결: `ws://localhost:8080/ws`

#### Subscribe (구독)
- `/sub/room/{roomId}` - 특정 채팅방 메시지 구독

#### Publish (발행)
- `/pub/message` - 채팅 메시지 전송
- `/pub/request-recommendation` - 맛집 추천 요청

## 프로젝트 구조

```
src/main/java/com/example/onboarding/
├── config/                 # 설정 클래스
│   ├── AsyncConfig.java       # 비동기 처리 설정
│   ├── SwaggerConfig.java     # Swagger 문서화 설정
│   └── WebSocketConfig.java   # WebSocket 설정
├── controller/             # REST/WebSocket 컨트롤러
│   ├── ChatController.java    # WebSocket 채팅 컨트롤러
│   ├── ChatRoomController.java # 채팅방 REST API
│   └── HealthController.java  # 헬스 체크
├── dto/                    # 데이터 전송 객체
│   ├── ChatMessageDto.java
│   ├── ClaudeAnalysisResult.java
│   ├── RecommendationPromptDto.java
│   ├── RecommendationRequestDto.java
│   ├── RestaurantDto.java
│   └── SuggestionDto.java
├── entity/                 # JPA 엔티티
│   ├── ChatMessage.java
│   ├── ChatRoom.java
│   ├── ChatUser.java
│   ├── MessageType.java
│   └── Restaurant.java
├── repository/             # JPA 레포지토리
│   ├── ChatMessageRepository.java
│   ├── ChatRoomRepository.java
│   ├── ChatUserRepository.java
│   └── RestaurantRepository.java
├── service/                # 비즈니스 로직
│   ├── ChatService.java       # 채팅 서비스
│   ├── ClaudeService.java     # Claude API 통신
│   └── SuggestionService.java # 맛집 추천 서비스
└── websocket/              # WebSocket 관련
    ├── UserHandshakeInterceptor.java
    └── UserSessionChannelInterceptor.java
```

## 맛집 추천 프로세스

### 1단계: 대화 분석
1. 사용자가 채팅 메시지를 전송
2. 서버가 최근 10개 메시지를 포함한 대화 맥락 수집
3. Claude AI로 대화 분석 수행
4. 추천 필요 여부 판단 (신뢰도 0.6 이상)
5. 추천 가능 시 클라이언트에 알림 전송 (analysisId 포함)

### 2단계: 맛집 추천
1. 사용자가 추천 버튼 클릭 (analysisId 포함)
2. 서버가 캐시에서 분석 결과 조회
3. 지역 및 카테고리 기반 맛집 검색
4. 최대 5개 맛집 추천 결과 반환
5. 처리 완료 후 캐시 정리

## 개발 도구

### Swagger UI
애플리케이션 실행 후 다음 URL에서 API 문서 확인:
```
http://localhost:8080/swagger-ui.html
```

### 로깅
디버그 로깅 활성화 영역:
- WebSocket 통신
- 채팅 서비스
- 맟집 추천 서비스
- Spring Messaging 프레임워크

## 주의사항

1. **환경 변수**: `DB_PASSWORD`와 `ANTHROPIC_API_KEY`는 반드시 설정 필요
2. **데이터베이스**: PostgreSQL 데이터베이스가 실행 중이어야 함
3. **포트**: 기본 포트 8080이 사용 가능해야 함
4. **캐시 만료**: 분석 결과는 5분 후 자동 삭제됨

## 라이센스

이 프로젝트는 온보딩 해커톤을 위해 개발되었습니다.
