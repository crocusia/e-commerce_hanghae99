# 이커머스 서비스

이커머스 주문 및 결제 시스템 - 장바구니부터 결제 완료까지의 전체 주문 프로세스를 처리합니다.

## 📋 주요 기능
[requirements.md](docs/api/requirements.md)
### 1. 상품 관리
- 상품 목록 조회 (페이징)
- 상품 상세 정보 및 실시간 재고 확인

### 2. 장바구니 관리
- 장바구니 추가/수정/삭제
- 실시간 재고 확인
- 총 금액 계산

### 3. 주문 및 결제
- 장바구니 기반 주문 생성
- 비관적 락을 통한 재고 동시성 제어
- 쿠폰 할인 적용
- 잔액 차감 및 결제 처리
- 주문 취소/반품/교환

### 4. 선착순 쿠폰 발급
- 동시성 제어
- 발급 수량 제한 및 1인 1매 정책
- 중복 발급 방지

## 🔌 API 문서

### Swagger UI
- **로컬**: http://localhost:8080/swagger-ui.html

### 주요 엔드포인트

#### 🛍️ 상품 (Product)
```http
GET  /api/products              # 상품 목록 조회
GET  /api/products/{id}         # 상품 상세 조회
```

#### 🛒 장바구니 (Cart)
```http
GET    /api/carts/{userId}                      # 장바구니 조회
POST   /api/carts/{userId}/items                # 장바구니 상품 추가
PATCH  /api/carts/{userId}/items/{cartItemId}   # 수량 변경
DELETE /api/carts/{userId}/items/{cartItemId}   # 상품 삭제
```

#### 📦 주문 (Order)
```http
POST /api/orders                # 주문 생성
POST /api/orders/payment        # 결제 처리
POST /api/orders/{id}/cancel    # 주문 취소
GET  /api/orders/{id}           # 주문 상세 조회
```

#### 🎫 쿠폰 (Coupon)
```http
POST /api/user-coupons/issue                        # 쿠폰 발급 (선착순)
GET  /api/user-coupons/users/{userId}               # 사용자 쿠폰 목록
```

자세한 API 명세: [Swagger UI](http://localhost:8080/swagger-ui.html)

## 🚀 실행 방법
```bash
# 애플리케이션 실행
./gradlew bootRun

# API 문서 확인
open http://localhost:8080/swagger-ui.html
```

## 🗄️ 데이터베이스 구조
[database-schema.md](docs/api/database-schema.md) 
<br>

**총 7개 테이블**

| 테이블 | 설명 |
|--------|------|
| `users` | 사용자 정보 및 잔액 관리 |
| `products` | 상품 정보 및 재고 관리 |
| `cart_items` | 장바구니 상품 정보 |
| `orders` | 주문 기본 정보 (주문번호, 금액, 상태) |
| `order_items` | 주문 상품 상세 (가격 스냅샷, 수량, 상태) |
| `coupons` | 쿠폰 정책 및 발급 수량 관리 |
| `user_coupons` | 사용자별 쿠폰 발급/사용 내역 |

**주요 관계**
- User 1:N Order, CartItem
- Order 1:N OrderItem
- Coupon 1:N UserCoupon


## 📊 주요 플로우
[sequence-diagram.md](docs/api/sequence-diagram.md)
### 1. 주문 생성 및 결제 처리
```
장바구니 조회 → 재고 차감(비관적 락) → 주문 생성 → 쿠폰 적용 → 잔액 차감 → 결제 완료
```

### 2. 선착순 쿠폰 발급
```
동시성 제어(락) → 수량 검증 → 중복 발급 확인 → 쿠폰 발급 → 발급 수량 증가
```

### 3. 장바구니 관리
```
상품 조회 → 재고 확인 → 장바구니 추가 → 수량 변경 → 주문 진행
```

## 📚 관련 문서

- [요구사항 명세](docs/api/requirements.md)
- [데이터베이스 스키마](docs/api/database-schema.md)
- [시퀀스 다이어그램](docs/api/sequence-diagram.md)
