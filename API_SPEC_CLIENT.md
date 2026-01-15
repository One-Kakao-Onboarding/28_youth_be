# 클라이언트용 WebSocket API 명세서

## 개요

맛집 추천 시스템이 **2단계 플로우**로 변경되었습니다.

### 변경 사항 요약

**이전:**
- 서버가 메시지를 분석하고 자동으로 맛집을 추천

**현재:**
- **1단계:** 서버가 메시지를 분석하고 "추천 가능" 알림을 클라이언트에게 전송
- **2단계:** 클라이언트가 원할 때 추천 요청 → 서버가 맛집 추천

---

## WebSocket 연결 정보

### 엔드포인트
```
ws://192.168.8.180:8080/ws-chat
```

### 연결 헤더
```javascript
{
  'X-User-Id': 'user-uuid',
  'X-Nickname': '사용자닉네임'
}
```

---

## 1. 새로운 구독 채널

### 1-1. 추천 알림 채널 (신규)

**채널:** `/user/queue/recommendation-prompt`

**설명:** 서버가 Claude AI 분석 결과 맛집 추천이 가능하다고 판단했을 때, 클라이언트에게 알림을 보냅니다.

**응답 DTO 구조:**
```typescript
interface RecommendationPromptDto {
  analysisId: string;      // 분석 결과 식별자 (추천 요청 시 필요)
  location: string;        // 추출된 지역 (예: "판교", "강남")
  mealType: string;        // 식사 종류 (예: "점심", "저녁")
  confidence: number;      // 신뢰도 (0.6 ~ 1.0)
  time: string;           // 전송 시각 (예: "오후 2:30")
}
```

**응답 예시:**
```json
{
  "analysisId": "a3f7b2c1-4e5d-6a7b-8c9d-0e1f2a3b4c5d",
  "location": "판교",
  "mealType": "점심",
  "confidence": 0.85,
  "time": "오후 2:30"
}
```

**클라이언트 처리:**
1. 이 메시지를 받으면 사용자에게 "맛집 추천을 받으시겠습니까?" UI 표시
2. 사용자가 "예"를 선택하면 `analysisId`를 저장해두고 추천 요청 전송

---

### 1-2. 맛집 추천 채널 (기존, 동작 변경)

**채널:** `/user/queue/suggestions`

**설명:** 클라이언트가 추천을 요청했을 때만 맛집 리스트를 전송합니다. (이전에는 자동 전송)

**응답 DTO 구조:**
```typescript
interface SuggestionDto {
  type: "card";
  message: string;
  cardData: {
    title: string;          // 예: "판교 한식 맛집 추천 리스트"
    image: string;          // 이미지 URL
    restaurants: Restaurant[];
  };
  time: string;
}

interface Restaurant {
  id: number;
  name: string;
  category: string;
  locationText: string;
  address: string;          // locationText와 동일
  description: string;
  rating: number;           // 평점 (예: 4.7)
  image: string;            // 이미지 URL
  distance: string;         // 거리 정보 (예: "도보 5분")
}
```

**응답 예시:**
```json
{
  "type": "card",
  "message": "판교에서 점심 먹기 좋은 맛집을 추천해드립니다!",
  "cardData": {
    "title": "판교 한식 맛집 추천 리스트",
    "image": "/images/restaurant-map.jpg",
    "restaurants": [
      {
        "id": 1,
        "name": "판교 삼겹살집",
        "category": "한식 • 고기",
        "locationText": "판교역 1번 출구",
        "address": "판교역 1번 출구",
        "description": "두툼한 삼겹살과 함께하는 특별한 식사",
        "rating": 4.7,
        "image": "/images/samgyeopsal.jpg",
        "distance": "도보 5분"
      }
    ]
  },
  "time": "오후 2:31"
}
```

---

### 1-3. 에러 채널 (기존)

**채널:** `/user/queue/errors`

**설명:** 처리 중 오류 발생 시 에러 메시지 전송

**응답 예시:**
```json
{
  "message": "분석 결과를 찾을 수 없습니다. 다시 시도해주세요."
}
```

---

## 2. 새로운 발행 엔드포인트

### 2-1. 맛집 추천 요청 (신규)

**엔드포인트:** `/pub/request-recommendation`

**설명:** 클라이언트가 서버에게 맛집 추천을 요청합니다.

**요청 DTO 구조:**
```typescript
interface RecommendationRequestDto {
  analysisId: string;      // 서버가 보낸 RecommendationPromptDto의 analysisId
  userId: string;          // 현재 사용자 ID (선택사항, WebSocket 세션에서 자동 추출)
}
```

**요청 예시:**
```json
{
  "analysisId": "a3f7b2c1-4e5d-6a7b-8c9d-0e1f2a3b4c5d"
}
```

**주의사항:**
- `analysisId`는 5분간 유효합니다. 만료된 경우 에러 메시지를 받습니다.
- 동일한 `analysisId`로 중복 요청 시 에러 메시지를 받습니다.

---

### 2-2. 채팅 메시지 전송 (기존)

**엔드포인트:** `/pub/message`

**설명:** 일반 채팅 메시지 전송

**요청 DTO 구조:**
```typescript
interface ChatMessageDto {
  roomId: string;
  content: string;
  type?: "ENTER" | "TALK" | "LEAVE";  // 기본값: "TALK"
}
```

---

## 3. 전체 플로우

### 시나리오: 사용자가 "판교에서 점심 먹을 곳 추천해줘" 메시지 전송

```
1. [클라이언트] /pub/message로 채팅 메시지 전송
   {
     "roomId": "room-123",
     "content": "판교에서 점심 먹을 곳 추천해줘",
     "type": "TALK"
   }

2. [서버 → 클라이언트] /sub/room/room-123로 채팅 메시지 브로드캐스트
   {
     "id": 456,
     "roomId": "room-123",
     "senderId": "user-uuid",
     "senderNickname": "홍길동",
     "content": "판교에서 점심 먹을 곳 추천해줘",
     "type": "TALK",
     "createdAt": "2026-01-15T14:30:00"
   }

3. [서버 → 클라이언트] /user/queue/recommendation-prompt로 추천 알림 전송
   {
     "analysisId": "a3f7b2c1-4e5d-6a7b-8c9d-0e1f2a3b4c5d",
     "location": "판교",
     "mealType": "점심",
     "confidence": 0.85,
     "time": "오후 2:30"
   }

4. [클라이언트] 사용자에게 "맛집 추천을 받으시겠습니까?" UI 표시
   - 사용자가 "예" 선택

5. [클라이언트] /pub/request-recommendation로 추천 요청 전송
   {
     "analysisId": "a3f7b2c1-4e5d-6a7b-8c9d-0e1f2a3b4c5d"
   }

6. [서버 → 클라이언트] /user/queue/suggestions로 맛집 추천 전송
   {
     "type": "card",
     "message": "판교에서 점심 먹기 좋은 맛집을 추천해드립니다!",
     "cardData": {
       "title": "판교 한식 맛집 추천 리스트",
       "restaurants": [...]
     },
     "time": "오후 2:31"
   }
```

---

## 4. 클라이언트 구현 가이드

### STOMP 클라이언트 설정 (예시: JavaScript/TypeScript)

```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://192.168.8.180:8080/ws-chat'),
  connectHeaders: {
    'X-User-Id': userId,
    'X-Nickname': nickname,
  },
  onConnect: () => {
    console.log('WebSocket 연결됨');

    // 1. 채팅방 메시지 구독
    client.subscribe(`/sub/room/${roomId}`, (message) => {
      const chatMessage = JSON.parse(message.body);
      // 채팅 UI 업데이트
    });

    // 2. 추천 알림 구독 (신규)
    client.subscribe('/user/queue/recommendation-prompt', (message) => {
      const prompt = JSON.parse(message.body);
      // "맛집 추천을 받으시겠습니까?" UI 표시
      showRecommendationPrompt(prompt);
    });

    // 3. 맛집 추천 구독
    client.subscribe('/user/queue/suggestions', (message) => {
      const suggestion = JSON.parse(message.body);
      // 맛집 카드 UI 표시
      showRestaurantCard(suggestion);
    });

    // 4. 에러 메시지 구독
    client.subscribe('/user/queue/errors', (message) => {
      const error = JSON.parse(message.body);
      console.error('서버 에러:', error.message);
    });
  },
});

client.activate();
```

### 메시지 전송 함수

```typescript
// 채팅 메시지 전송
function sendMessage(content: string) {
  client.publish({
    destination: '/pub/message',
    body: JSON.stringify({
      roomId: roomId,
      content: content,
      type: 'TALK',
    }),
  });
}

// 맛집 추천 요청 (신규)
function requestRecommendation(analysisId: string) {
  client.publish({
    destination: '/pub/request-recommendation',
    body: JSON.stringify({
      analysisId: analysisId,
    }),
  });
}
```

### UI 처리 예시

```typescript
// 추천 알림 받았을 때
function showRecommendationPrompt(prompt: RecommendationPromptDto) {
  const message = `${prompt.location}에서 ${prompt.mealType} 맛집을 추천받으시겠습니까?`;

  // 사용자에게 확인 대화상자 표시
  if (confirm(message)) {
    // 사용자가 "예" 선택 시 추천 요청
    requestRecommendation(prompt.analysisId);
  }
  // 사용자가 "아니오" 선택 시 아무것도 하지 않음 (알림 무시)
}

// 맛집 추천 받았을 때
function showRestaurantCard(suggestion: SuggestionDto) {
  const { cardData } = suggestion;

  // 카드 UI 렌더링
  renderCard({
    title: cardData.title,
    image: cardData.image,
    restaurants: cardData.restaurants.map(r => ({
      name: r.name,
      category: r.category,
      rating: r.rating,
      image: r.image,
      distance: r.distance,
    })),
  });
}
```

---

## 5. 주의사항

### 타이밍
- `analysisId`는 생성 후 **5분간 유효**합니다.
- 5분이 지난 후 추천을 요청하면 에러 메시지를 받습니다.

### 중복 요청
- 동일한 `analysisId`로 여러 번 요청하면 에러 메시지를 받습니다.
- 한 번 추천을 받으면 해당 분석 결과는 소진됩니다.

### 신뢰도
- 서버는 Claude AI 분석 신뢰도가 0.6 이상일 때만 알림을 보냅니다.
- 낮은 신뢰도의 메시지는 일반 대화로 처리되어 알림이 오지 않습니다.

### 에러 처리
- `/user/queue/errors` 채널을 반드시 구독하여 에러 처리를 해야 합니다.
- 가능한 에러:
  - "분석 결과를 찾을 수 없습니다" (만료 또는 잘못된 analysisId)
  - "이미 처리된 분석입니다" (중복 요청)
  - "추천할 맛집을 찾을 수 없습니다" (검색 결과 없음)

---

## 6. 테스트 체크리스트

- [ ] WebSocket 연결 성공
- [ ] 채팅 메시지 송수신
- [ ] 추천 알림 수신 (`/user/queue/recommendation-prompt`)
- [ ] 추천 요청 전송 (`/pub/request-recommendation`)
- [ ] 맛집 추천 수신 (`/user/queue/suggestions`)
- [ ] 에러 메시지 수신 및 처리
- [ ] 5분 후 만료된 analysisId로 요청 시 에러 처리
- [ ] 중복 요청 시 에러 처리
- [ ] 일반 대화 시 추천 알림 미수신 확인

---

## 7. 문의사항

백엔드 관련 문의사항은 백엔드 개발팀에게 전달해주세요.

**서버 정보:**
- 로컬: `http://localhost:8080`
- 네트워크: `http://192.168.8.180:8080`
- WebSocket: `ws://192.168.8.180:8080/ws-chat`
