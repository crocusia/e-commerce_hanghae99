# STEP 13: Redis Sorted Set 기반 상품 랭킹 시스템

## 📋 개요

**목표**: 최근 3일 간 판매량 기준 인기 상품 랭킹 제공

**핵심 전략**:
- 실시간 카운팅: 주문 시 Redis에 즉시 반영
- 일별 랭킹 제공: 매일 새벽 3시 스냅샷 갱신 (하루 1회)
- 고객 조회: 스냅샷 기반 빠른 응답

---

## 🎯 Sorted Set 선택 이유

### 자료구조 비교

| 자료구조 | 업데이트 | 조회 | 순위 조회 | 메모리 |
|---------|---------|------|----------|--------|
| **Sorted Set** | **O(log N)** | **O(log N + M)** | **O(log N)** | **높음** |
| Hash | O(1) | O(N log N) | O(N log N) | 중간 |
| List | O(N) | O(1) | O(N) | 높음 |

### 핵심 장점

**1. 자동 정렬**
```redis
ZADD product:ranking:2025-12-05 100 "1"
ZADD product:ranking:2025-12-05 200 "2"
# → 자동으로 score 순 정렬 유지
```

**2. 원자적 증가**
```redis
ZINCRBY product:ranking:2025-12-05 1 "1"
# → 동시성 제어 불필요, 안전한 증가
```

**3. 효율적 조회**
```redis
ZREVRANGE product:ranking:2025-12-05 0 9 WITHSCORES
# → Top 10 조회: O(log N + 10)
```

**4. 3일 통합 (ZUNIONSTORE)**
```redis
ZUNIONSTORE snapshot 3 key1 key2 key3
# → 단일 명령으로 3일 합산
```

---

## 🏗️ 인기상품 파이프라인

### 전체 플로우

```
┌──────────────────────────────────────────────────┐
│                 1. 주문 결제 완료                 │
└──────────────────────────────────────────────────┘
                      ↓
            PaymentCompletedEvent
                      ↓
┌──────────────────────────────────────────────────┐
│         2. 실시간 카운팅 (비동기)                 │
│    ProductSalesEventListener                     │
└──────────────────────────────────────────────────┘
         ↓                           ↓
[product:sales]          [product:ranking:{date}]
(전체 누적)               (일별, TTL 4일)
ZINCRBY {id} {qty}       ZINCRBY {id} {qty}


┌──────────────────────────────────────────────────┐
│         3. 매일 새벽 3시 - 스냅샷 생성            │
│    SalesAggregationService                       │
└──────────────────────────────────────────────────┘
         ↓                           ↓
[DB 동기화]                  [스냅샷 생성]
product:sales               ZUNIONSTORE
    ↓                        ├─ 2025-12-05
ProductPopular               ├─ 2025-12-04
테이블 갱신                   └─ 2025-12-03
                                  ↓
                    product:ranking:3days:snapshot
                        (TTL 25시간)


┌──────────────────────────────────────────────────┐
│              4. 고객 랭킹 조회                    │
│    ProductService.getPopularProductsFromRedis()  │
└──────────────────────────────────────────────────┘
                      ↓
        product:ranking:3days:snapshot
              (하루 1회 갱신)
                      ↓
              O(log N) 빠른 조회
                      ↓
            인기 상품 리스트 반환
```

---

## 🔑 Redis 키 구조

### 1. 일별 랭킹 (실시간 카운팅)

```
Key: product:ranking:{date}
Type: Sorted Set
Score: 해당 날짜 판매량
Member: {productId}
TTL: 4일
갱신: 결제 완료 시 ZINCRBY
```

### 2. 3일 스냅샷 (고객 조회용)

```
Key: product:ranking:3days:snapshot
Type: Sorted Set
Score: 3일 합산 판매량
Member: {productId}
TTL: 25시간
갱신: 매일 새벽 3시 ZUNIONSTORE
```

### 3. 전체 누적 (DB 동기화용)

```
Key: product:sales
Type: Sorted Set
Score: 전체 누적 판매량
Member: {productId}
TTL: 없음
갱신: 결제 완료 시 ZINCRBY
```

---

## ✅ 스냅샷 없을 때 처리

### 기본 전략

```
스냅샷 조회
  ↓ 있으면
Top N 반환 (빠름, O(log N))
  ↓ 없으면
빈 리스트 반환
```

### 스냅샷 없는 시나리오

| 상황 | 발생 가능성 | 대응 |
|-----|-----------|------|
| 앱 재시작 직후 | 높음 (새벽 3시 이전) | 빈 리스트, 다음 배치 대기 |
| 배치 실패 | 낮음 | 빈 리스트, 모니터링 알림 |
| TTL 만료 (25시간) | 매우 낮음 | 빈 리스트, 배치 연속 실패 시 |

---

## 📊 성능 특성

### 시간 복잡도

| 연산 | 복잡도 | 빈도 |
|-----|--------|------|
| 판매량 증가 (ZINCRBY) | O(log N) | 주문마다 |
| 스냅샷 생성 (ZUNIONSTORE) | O(3N) | 매일 1회 |
| 고객 조회 (ZREVRANGE) | O(log N + M) | 언제든지 |


---
# STEP 14: 비동기 쿠폰 발급 시스템

## 📋 개요

**목표**: 초당 수천 건의 요청을 처리할 수 있는 선착순 100명 쿠폰 발급 시스템

**핵심 전략**:
- Redis 원자적 연산 활용 (Lua Script 없이)
- 분산 락 없이 정확한 수량 제어
- 비동기/대기열 기반 아키텍처
- 동기(요청 접수) → Redis 처리 → 비동기(Scheduler) → DB 벌크 처리

---

## 🔑 Redis 자료구조

### 핵심 4가지

| 역할 | 자료구조 | Key 패턴 | 연산 | 특징 |
|-----|---------|----------|------|------|
| 수량 예약 | String | `coupon:{id}:counter` | INCR/DECR | 원자적 증가, 롤백 가능 |
| 중복 체크 | Set | `coupon:{id}:issued:users` | SADD | 반환값 1(신규)/0(중복) |
| 대기열 | Sorted Set | `coupon:{id}:waiting:queue` | ZADD/ZPOPMIN | Score=timestamp(FIFO) |
| 발급 상태 | String | `coupon:{id}:user:{userId}:status` | SET/GET | TTL 24시간 |

### 자료구조 선택 이유

**1. String (Counter)**
- INCR/DECR: 원자적 수량 제어
- 롤백 가능: 실패 시 DECR로 복구
- 동시성 안전: 분산 락 불필요

**2. Set (중복 체크)**
- SADD 반환값: 1(신규), 0(중복)
- O(1) 조회: 빠른 중복 확인
- 원자적 추가: 동시 요청 안전

**3. Sorted Set (대기열)**
- Score 정렬: timestamp로 FIFO 보장
- ZPOPMIN: 원자적 배치 추출
- ZRANK: 대기 순번 조회 (UX)

---

## 🏗️ 시스템 아키텍처

### 전체 플로우
```
┌──────────────────────────────────────────────────┐
│             1. 사용자 발급 요청                    │
│    POST /api/coupons/{couponId}/issue            │
└──────────────────────────────────────────────────┘
                      ↓
┌──────────────────────────────────────────────────┐
│         2. CouponIssueFacade (동기)              │
│         - 쿠폰 유효성 검증                         │
│         - Redis 원자적 연산                       │
└──────────────────────────────────────────────────┘
         ↓              ↓              ↓
    [INCR]         [SADD]         [ZADD]
    수량 예약       중복 체크       대기열 추가
         ↓              ↓              ↓
  coupon:{id}:   coupon:{id}:   coupon:{id}:
    counter     issued:users  waiting:queue
  (롤백 가능)    (중복 방지)   (FIFO 보장)
         ↓
    [SET with TTL]
    상태 저장
         ↓
  coupon:{id}:user:{userId}:status = "PENDING"
         ↓
┌──────────────────────────────────────────────────┐
│                 즉시 응답 반환                     │
│       "쿠폰 발급 요청이 접수되었습니다"             │
└──────────────────────────────────────────────────┘


┌──────────────────────────────────────────────────┐
│    3. CouponIssueScheduler (비동기, 5초마다)     │
│    - 활성화된 쿠폰 목록 조회                       │
└──────────────────────────────────────────────────┘
         ↓
    [GET counter]
    남은 수량 계산
         ↓
 remainingCount = limit - redisCount
         ↓
   batchSize = min(remainingCount, 100, queueSize)
         ↓
    [ZPOPMIN]
    대기열에서 배치 추출
         ↓
┌──────────────────────────────────────────────────┐
│            4. DB 벌크 발급 처리                   │
│    - UserCoupon 엔티티 일괄 생성                  │
│    - Bulk INSERT to user_coupons                │
│    - Coupon.issuedQuantity 업데이트              │
└──────────────────────────────────────────────────┘
         ↓
    [SET (Pipeline)]
    상태 일괄 업데이트
         ↓
  coupon:{id}:user:{userId}:status = "ISSUED"
         ↓
┌──────────────────────────────────────────────────┐
│              5. 사용자 상태 조회                   │
│    GET /api/coupons/{couponId}/issue/status      │
└──────────────────────────────────────────────────┘
         ↓
    [GET status]
    Redis에서 상태 조회
         ↓
    PENDING → 대기 순번 포함 응답
    ISSUED → 발급 완료 응답
    NOT_REQUESTED → 요청 없음
    
---
## 🔑 Redis 키 구조

### 1. 수량 예약 (Counter)

```
Key: coupon:{couponId}:counter
Type: String (숫자)
Value: 현재 예약된 수량
연산: INCR (원자적 증가), DECR (롤백)
TTL: 없음 (쿠폰 종료 시 수동 삭제)
```

### 2. 중복 체크 (Set)

```
Key: coupon:{couponId}:issued:users
Type: Set
Member: userId (문자열)
연산: SADD (원자적 추가)
반환값: 1 (신규), 0 (중복)
TTL: 없음
```

### 3. 대기열 (Sorted Set)

```
Key: coupon:{couponId}:waiting:queue
Type: Sorted Set
Score: timestamp (밀리초)
Member: userId (문자열)
연산: ZADD (추가), ZPOPMIN (배치 추출)
정렬: Score 오름차순 (FIFO)
TTL: 없음
```

### 4. 발급 상태 (String)

```
Key: coupon:{couponId}:user:{userId}:status
Type: String
Value: "PENDING" | "ISSUED" | "FAILED"
연산: SET with TTL
TTL: 86400초 (24시간)
```


**1. 동기 요청 접수** (CouponIssueFacade):
```
1. 쿠폰 유효성 검증
2. INCR: 수량 예약 (롤백 가능)
3. SADD: 중복 체크 (실패 시 DECR)
4. ZADD: 대기열 추가 (timestamp score로 FIFO)
5. SET: 상태 저장 ("PENDING", TTL 24시간)
→ 즉시 응답: "발급 요청 접수 완료"
```

**2. 비동기 발급 처리** (CouponIssueScheduler):
```
1. 활성화된 쿠폰 조회
2. 남은 수량 계산 (Redis 카운터 기반)
3. 배치 크기 결정 (min(남은수량, 100, 대기열크기))
4. ZPOPMIN: 대기열에서 배치 추출
5. DB 벌크 INSERT: UserCoupon 일괄 생성
6. Coupon.issuedQuantity 업데이트
7. Pipeline: 상태 일괄 업데이트 ("ISSUED")
```

**3. 상태 조회** (CouponIssueQueryService):
```
1. Redis 상태 조회 (빠른 응답)
   - PENDING: 대기 순번 포함
   - ISSUED: 발급 완료
2. Redis 없으면 DB 조회
3. 둘 다 없으면 "NOT_REQUESTED"
```
---

## 📊 성능 분석

### 시간 복잡도

| 연산 | 복잡도 | 빈도 | 설명 |
|-----|--------|------|------|
| INCR | O(1) | 요청마다 | 수량 예약 |
| SADD | O(1) | 요청마다 | 중복 체크 |
| ZADD | O(log N) | 요청마다 | 대기열 추가 |
| ZPOPMIN | O(M log N) | 5초마다 | 배치 추출 (M=배치크기) |
| Bulk INSERT | O(M) | 5초마다 | DB 일괄 삽입 |