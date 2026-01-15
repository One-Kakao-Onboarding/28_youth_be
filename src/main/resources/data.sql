-- 맛집 더미 데이터 (판교, 잠실, 합정 지역)

-- 판교 맛집
INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('판교 삼겹살 명가', '한식 • 고기', '경기도 성남시 분당구 판교역로 166', '두툼하고 육즙 가득한 삼겹살을 숯불에 구워 제공하는 맛집. 회식이나 저녁 모임에 추천!', '판교,삼겹살,한식,회식,고기', 4.7, '/images/samgyeopsal.jpg', '도보 5분')
ON CONFLICT DO NOTHING;

INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('판교 초밥 스시로', '일식 • 스시', '경기도 성남시 분당구 백현로 20', '신선한 재료로 만든 초밥과 사시미를 합리적인 가격에 즐길 수 있는 일식당.', '판교,초밥,일식,스시,회', 4.5, '/images/sushi.jpg', '도보 7분')
ON CONFLICT DO NOTHING;

INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('판교 파스타하우스', '양식 • 이탈리안', '경기도 성남시 분당구 판교역로 235', '수제 파스타와 리조또를 전문으로 하는 이탈리안 레스토랑. 데이트 코스로 추천.', '판교,파스타,양식,이탈리안,데이트', 4.8, '/images/pasta.jpg', '도보 10분')
ON CONFLICT DO NOTHING;

-- 잠실 맛집
INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('잠실 마라탕', '중식 • 마라탕', '서울 송파구 올림픽로 240', '얼얼하고 진한 맛의 마라탕을 즐길 수 있는 중식당. 매운 음식을 좋아하는 분들께 추천!', '잠실,마라탕,중식,매운음식,훠궈', 4.4, '/images/malatang.jpg', '도보 3분')
ON CONFLICT DO NOTHING;

INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('잠실 떡볶이 골목', '분식 • 떡볶이', '서울 송파구 백제고분로 7길', '옛날 떡볶이의 그 맛을 그대로 재현한 분식집. 튀김과 순대도 일품!', '잠실,떡볶이,분식,튀김,순대', 4.6, '/images/tteokbokki.jpg', '도보 8분')
ON CONFLICT DO NOTHING;

INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('잠실 한우 숯불구이', '한식 • 한우', '서울 송파구 올림픽로 300', '최상급 한우를 숯불에 구워 제공하는 고급 한식당. 특별한 날 방문하기 좋음.', '잠실,한우,고기,한식,숯불구이', 4.9, '/images/hanwoo.jpg', '도보 6분')
ON CONFLICT DO NOTHING;

INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('잠실 돈까스 명가', '일식 • 돈까스', '서울 송파구 송파대로 111', '바삭하고 두툼한 돈까스를 제공하는 일식당. 점심 세트 메뉴가 인기!', '잠실,돈까스,일식,점심,세트메뉴', 4.7, '/images/donkatsu.jpg', '도보 4분')
ON CONFLICT DO NOTHING;

-- 합정 맛집
INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('합정 브런치 카페', '카페 • 브런치', '서울 마포구 양화로 45', '여유로운 주말 브런치를 즐길 수 있는 감성 카페. 에그베네딕트와 플랫화이트가 시그니처 메뉴.', '합정,브런치,카페,커피,주말', 4.8, '/images/brunch-cafe.jpg', '도보 12분')
ON CONFLICT DO NOTHING;

INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('합정 족발 골목', '한식 • 족발', '서울 마포구 동교로 120', '쫄깃하고 부드러운 족발을 제공하는 전통 한식당. 소주 한 잔과 함께 즐기기 좋음.', '합정,족발,한식,보쌈,소주', 4.5, '/images/jokbal.jpg', '도보 9분')
ON CONFLICT DO NOTHING;

INSERT INTO restaurants (name, category, location_text, description, keywords, rating, image_url, distance_text)
VALUES ('합정 타코 트럭', '멕시칸 • 타코', '서울 마포구 합정로 77', '정통 멕시칸 타코와 부리또를 제공하는 푸드트럭 스타일 레스토랑. 가볍게 한 끼 해결하기 좋음.', '합정,타코,멕시칸,부리또,간식', 4.3, '/images/taco.jpg', '도보 15분')
ON CONFLICT DO NOTHING;

-- 기본 채팅방 생성
INSERT INTO chat_rooms (name, created_at)
VALUES ('일반 채팅방', NOW())
ON CONFLICT DO NOTHING;
