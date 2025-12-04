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

### 왜 빈 리스트인가?

**목표와 일치**
- "실시간 관리 불필요, 하루 1회 갱신"
- 실시간 ZUNIONSTORE는 목표와 상충
- 빈 리스트 = 아직 집계 안 됨 (명확한 의미)

**Redis 부하 방지**
- Fallback으로 ZUNIONSTORE 실행 시 O(3N) 비용
- 정상 운영 시 스냅샷 항상 존재
- 일시적 상황에서만 빈 리스트 (허용 가능)

**코드 간소화**
- 복잡한 Fallback 로직 불필요
- 예외 처리 단순
- 유지보수 용이

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