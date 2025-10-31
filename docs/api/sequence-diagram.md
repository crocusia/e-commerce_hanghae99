# 이커머스 플랫폼 API 시퀀스 다이어그램

## 목차
- [1. 상품 API](#1-상품-api)
- [2. 장바구니 API](#2-장바구니-api)
- [3. 쿠폰 API](#3-쿠폰-api)
- [4. 주문 및 결제 API](#4-주문-및-결제-api)
- [5. 주문 상품 관리 API](#5-주문-상품-관리-api)

---

## 1. 상품 API
### 1.1 상품 목록 조회
- `GET /api/products` 

### 1.2 상품 상세 조회
- `GET /api/products/{productId}`
 ```mermaid
sequenceDiagram
    actor User as 사용자
    participant Controller as CartController
    participant CartService as CartService
    participant ProductService as ProductService
    participant DB as Database

    rect rgb(240, 255, 240)
        Note over User,DB: 상품 조회 및 재고 확인
        
        User->>Controller: GET /api/products/{productId}
        Controller->>ProductService: getProduct(productId)
        
        ProductService->>DB: SELECT * FROM products<br/>WHERE id = ?
        DB-->>ProductService: Product (id: 101, name: "노트북",<br/>price: 1500000, stockQty: 30)
        
        ProductService-->>Controller: ProductResponse
        Controller->>User: 200 OK<br/>상품 정보 + 재고 수량
    end
``` 

---

## 2. 장바구니 API

### 주요 엔드포인트
- `GET /api/carts/{userId}` - 장바구니 조회
- `POST /api/carts/{userId}/items` - 장바구니에 상품 추가
- `PUT /api/carts/{userId}/items/{cartItemId}` - 수량 변경
- `DELETE /api/carts/{userId}/items/{cartItemId}` - 상품 삭제
- `DELETE /api/carts/{userId}` - 장바구니 전체 비우기

### 2.1 장바구니 조회
- `GET /api/carts/{userId}`

### 2.2 장바구니에 상품 추가
- `POST /api/carts/{userId}/items`
```mermaid
sequenceDiagram
    actor User as 사용자
    participant Controller as CartController
    participant CartService as CartService
    participant ProductService as ProductService
    participant DB as Database

    rect rgb(230, 240, 255)
        Note over User,DB: 장바구니 추가
        
        User->>Controller: POST /api/cart<br/>{userId: 1, productId: 101, quantity: 2}
        Controller->>CartService: addToCart(request)
        
        Note over CartService,DB: 🔄 @Transactional 시작
        
        CartService->>DB: SELECT * FROM products<br/>WHERE id = 101
        DB-->>CartService: Product (stockQty: 30)
        
        alt 재고 부족
            CartService-->>Controller: InsufficientStockException
            Controller->>User: 400 Bad Request<br/>"재고가 부족합니다"
        else 재고 충분
            CartService->>DB: SELECT * FROM cart_items<br/>WHERE user_id = 1 AND product_id = 101
            
            alt 이미 장바구니에 존재
                DB-->>CartService: CartItem (quantity: 1)
                CartService->>DB: UPDATE cart_items<br/>SET quantity = quantity + 2<br/>WHERE user_id = 1 AND product_id = 101
            else 새로운 상품
                DB-->>CartService: null
                CartService->>DB: INSERT INTO cart_items<br/>(user_id, product_id, quantity)
            end
            
            Note over CartService,DB: ✅ @Transactional 커밋
            
            CartService-->>Controller: CartItemResponse
            Controller->>User: 200 OK<br/>"장바구니에 추가되었습니다"
        end
    end
```

### 2.3 장바구니 수량 변경 

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Controller as CartController
    participant CartService as CartService
    participant ProductService as ProductService
    participant DB as Database

    rect rgb(255, 245, 230)
        Note over User,DB:  수량 변경
        
        User->>Controller: PATCH /api/cart/{cartItemId}<br/>{quantity: 5}
        Controller->>CartService: updateQuantity(cartItemId, quantity)
        
        Note over CartService,DB:  @Transactional 시작
        
        CartService->>DB: SELECT ci.*, p.stock_qty<br/>FROM cart_items ci<br/>JOIN products p ON ci.product_id = p.id<br/>WHERE ci.id = ?
        DB-->>CartService: CartItem + Product (stockQty: 30)
        
        alt 재고 부족
            CartService-->>Controller: InsufficientStockException
            Controller->>User: 400 Bad Request<br/>"재고가 부족합니다 (현재 재고: 30)"
        else 재고 충분
            CartService->>DB: UPDATE cart_items<br/>SET quantity = 5<br/>WHERE id = ?
            
            Note over CartService,DB:  @Transactional 커밋
            
            CartService-->>Controller: CartItemResponse
            Controller->>User: 200 OK<br/>"수량이 변경되었습니다"
        end
    end
```

### 2.4 장바구니 상품 삭제
- `DELETE /api/carts/{userId}/items/{cartItemId}` - 상품 삭제

### 2.5 장바구니 삭제
- `DELETE /api/carts/{userId}` - 장바구니 전체 비우기

---

## 3. 쿠폰 API

### 3.1 쿠폰 목록 조회
- `GET /api/coupons` 
### 3.2 특정 쿠폰 상세 조회
- `GET /api/coupons/{couponId}`
### 3.3 선착순 쿠폰 발급 
- `POST /api/user-coupons/issue`

```mermaid
sequenceDiagram
    actor User as 사용자 (1000명 동시 요청)
    participant Controller as UserCouponController
    participant Service as UserCouponService
    participant Redis as Redis<br/>(분산 락)
    participant DB as Database

    User->>Controller: POST /api/user-coupons/issue<br/>{userId: 1, couponId: 100}
    Controller->>Service: issueCoupon(request)
    
    rect rgb(255, 240, 240)
        Note over Service,Redis: 동시성 제어
        Service->>Redis: tryLock(waitTime=3s, leaseTime=10s)<br/>coupon:100:lock {uuid}
        
        alt 락 획득 실패
            Redis-->>Service: false (타임아웃)
            Service-->>Controller: CouponIssueLockException
            Controller->>User: 503 Service Unavailable<br/>
        else 락 획득 성공
            Redis-->>Service: true (락 획득 성공) 

        end
    end
    
    rect rgb(240, 255, 240)
        Note over Service,DB:  중복 발급 확인 (1인 1매)
        Service->>DB: SELECT COUNT(*) FROM user_coupons<br/>WHERE user_id = 1 AND coupon_id = 100
        
        alt 이미 발급받음
            DB-->>Service: count = 1
            Service->>Redis: DEL coupon:100:lock (락 해제)
            Service-->>Controller: DuplicateCouponException
            Controller->>User: 409 Conflict<br/>"이미 발급받은 쿠폰입니다"
        else 미발급
            DB-->>Service: count = 0 
        end
    end
    
    rect rgb(240, 240, 255)
        Note over Service,DB:  쿠폰 잔여 수량 확인
        Service->>DB: SELECT * FROM coupons<br/>WHERE id = 100 FOR UPDATE
        DB-->>Service: Coupon (totalQty: 1000, issuedQty: 999)
        
        Service->>Service: 잔여 수량 계산<br/>remaining = 1000 - 999 = 1
        
        alt 수량 마감 (선착순 탈락)
            Note right of Service: 수량 모두 소진<br/>선착순 마감
            Service->>Redis: DEL coupon:100:lock
            Service-->>Controller: CouponSoldOutException
            Controller->>User: 410 Gone<br/>"쿠폰이 모두 소진되었습니다"
        else 발급 가능 (remaining > 0)
            Note over Service,DB: 쿠폰 발급 처리
            
            Service->>DB: INSERT INTO user_coupons<br/>(user_id, coupon_id, status, issued_at)
            Service->>DB: UPDATE coupons<br/>SET issued_qty = 1000 WHERE id = 100
            
            DB-->>Service: 저장 완료
            
            Service->>Redis: DEL coupon:100:lock (락 해제)
            Redis-->>Service: 락 해제 완료
            
            Service-->>Controller: UserCouponResponse 
            Controller->>User: 201 Created<br/>"쿠폰이 발급되었습니다"
        end
    end

```

### 3.4 사용자 보유 쿠폰 조회
- `GET /api/user-coupons/users/{userId}`
### 3.5 사용자 쿠폰 상세 조회
- `GET /api/user-coupons/users/{userId}/coupons/{userCouponId}`

---

## 4. 주문 및 결제 API

### 4.1 주문 생성
- `POST /api/orders`

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Controller as OrderController
    participant OrderService as OrderService
    participant PaymentService as PaymentService
    participant DB as Database

    rect rgb(230, 240, 255)
        Note over User,DB: 1단계: 주문 생성 (재고 차감)
        
        User->>Controller: POST /api/orders<br/>{userId: 1}
        Controller->>OrderService: createOrder(request)
        
        Note over OrderService,DB:  @Transactional 시작
        
        OrderService->>DB: SELECT * FROM cart_items WHERE user_id = 1
        DB-->>OrderService: List<CartItem> (3개)
        
        loop 각 장바구니 아이템
            Note over OrderService,DB: 비관적 락으로 재고 확인 및 차감
            OrderService->>DB: SELECT * FROM products<br/>WHERE id = ? FOR UPDATE
            DB-->>OrderService: Product (LOCKED, stockQty: 50)
            
            alt 재고 부족
                Note over OrderService: 트랜잭션 롤백
                OrderService-->>Controller: InsufficientStockException
                Controller->>User: 400 Bad Request<br/>"재고가 부족합니다"
            else 재고 충분
                OrderService->>DB: UPDATE products<br/>SET stock_qty = stock_qty - ?<br/>WHERE id = ?
            end
        end
        
        OrderService->>DB: INSERT INTO orders<br/>(user_id, total_amount, status='PENDING')
        DB-->>OrderService: Order (id: 1001)
        
        OrderService->>DB: INSERT INTO order_items<br/>(order_id, product_id, quantity, unit_price)
        
        OrderService->>DB: DELETE FROM cart_items<br/>WHERE user_id = 1
        
        Note over OrderService,DB: @Transactional 커밋
        
        OrderService-->>Controller: OrderResponse
        Controller->>User: 201 Created<br/>"주문 생성 완료 (주문번호: 1001)"
    end
    
```
### 4.2 주문 목록 조회
- `GET /api/orders`
### 4.3 주문 상세 조회
- `GET /api/orders/{orderId}` 
### 4.4 주문 취소
- `POST /api/orders/{orderId}/cancel`
### 4.5 결제 처리
- `POST /api/orders/payment`

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Controller as OrderController
    participant OrderService as OrderService
    participant PaymentService as PaymentService
    participant DB as Database

    rect rgb(255, 245, 230)
        Note over User,DB: 2단계: 결제 처리 (쿠폰 적용 + 잔액 차감)
        
        User->>Controller: POST /api/orders/payment<br/>{orderId: 1001, userCouponId: 5}
        Controller->>PaymentService: processPayment(request)
        
        Note over PaymentService,DB: @Transactional 시작
        
        PaymentService->>DB: SELECT * FROM orders<br/>WHERE id = 1001
        DB-->>PaymentService: Order (status: PENDING, totalAmount: 50000)
        
        PaymentService->>PaymentService: finalAmount = 50000
        
        opt 쿠폰 사용
            PaymentService->>DB: SELECT * FROM user_coupons<br/>WHERE id = 5 AND user_id = 1
            DB-->>PaymentService: UserCoupon (discountPrice: 5000, status: UNUSED)
            
            PaymentService->>PaymentService: 쿠폰 유효성 검증<br/>✓ 유효기간 ✓ 최소주문금액 ✓ 미사용
            
            alt 쿠폰 유효
                PaymentService->>PaymentService: finalAmount = 50000 - 5000 = 45000
            else 쿠폰 무효
                PaymentService-->>Controller: InvalidCouponException
                Controller->>User: 400 Bad Request<br/>"쿠폰을 사용할 수 없습니다"
            end
        end
        
        PaymentService->>DB: SELECT * FROM users<br/>WHERE id = 1
        DB-->>PaymentService: User (balance: 100000)
        
        alt 잔액 부족
            PaymentService-->>Controller: InsufficientBalanceException
            Controller->>User: 400 Bad Request<br/>"잔액이 부족합니다"
        else 잔액 충분
            PaymentService->>DB: UPDATE users<br/>SET balance = balance - 45000<br/>WHERE id = 1
            
            PaymentService->>DB: UPDATE orders<br/>SET status = 'PAYMENT_COMPLETED',<br/>final_amount = 45000<br/>WHERE id = 1001
            
            opt 쿠폰 사용
                PaymentService->>DB: UPDATE user_coupons<br/>SET status = 'USED',<br/>order_id = 1001<br/>WHERE id = 5
            end
            
            Note over PaymentService,DB: @Transactional 커밋
            
            PaymentService-->>Controller: OrderResponse
            Controller->>User: 200 OK<br/>"결제 완료"
        end
    end
```
---

## 5. 주문 상품 관리 API

### 5.1 주문 상품 상세 조회
- `GET /api/order-items/{orderItemId}`
### 5.2 주문의 상품 목록 조회
- `GET /api/order-items/orders/{orderId}`
### 5.3 주문 상품 취소 
- `POST /api/order-items/{orderItemId}/cancel`
### 5.4 주문 상품 교환
- `POST /api/order-items/{orderItemId}/exchange`
### 5.5 주문 상품 반품
- `POST /api/order-items/{orderItemId}/return`
