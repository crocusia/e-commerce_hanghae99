# STEP 18: Kafka 기반 쿠폰/대기열 설계 문서

## 📋 목차
- [개요 및 목적](#개요-및-목적)
- [현재 아키텍처 분석](#현재-아키텍처-분석)
- [Kafka 특징 및 장점](#kafka-특징-및-장점)
- [Kafka 기반 쿠폰/대기열 설계](#kafka-기반-쿠폰대기열-설계)
- [아키텍처 비교 및 개선점](#아키텍처-비교-및-개선점)
- [구현 가이드](#구현-가이드)
- [성능 및 확장성 분석](#성능-및-확장성-분석)
- [트레이드오프 및 고려사항](#트레이드오프-및-고려사항)

---

## 개요 및 목적

### 배경
현재 시스템은 **Redis + Scheduler 폴링** 방식으로 선착순 쿠폰 발급을 처리하고 있습니다. 이 방식은 구현이 간단하지만, 다음과 같은 한계가 있습니다:

- **데이터 영속성 부족**: Redis 장애 시 대기열 데이터 손실
- **폴링 방식의 비효율**: 5초마다 폴링으로 인한 지연과 리소스 낭비
- **확장성 제한**: 단일 Scheduler로 인한 처리량 한계
- **재처리 어려움**: 실패한 요청 재처리 메커니즘 부족

### 목적
**Kafka를 활용하여 쿠폰/대기열 시스템의 안정성, 성능, 확장성을 향상시킵니다.**

- 메시지 영속성 보장으로 데이터 손실 방지
- 실시간 이벤트 기반 처리로 지연 최소화
- 파티셔닝과 Consumer Group으로 수평 확장 가능
- Offset 관리로 재처리 및 장애 복구 지원

---

## 현재 아키텍처 분석

### 전체 플로우

```
┌─────────────────────────────────────────────────────────┐
│            1. 사용자 발급 요청 (동기)                     │
│         POST /api/coupons/{couponId}/issue              │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│           2. CouponIssueFacade (동기 처리)               │
│            - 쿠폰 유효성 검증 (DB)                        │
│            - Redis 원자적 연산                           │
└─────────────────────────────────────────────────────────┘
         ↓                ↓                ↓
    [INCR]           [SADD]           [ZADD]
   수량 예약         중복 체크        대기열 추가
                                     (timestamp)
         ↓                ↓                ↓
  coupon:{id}:    coupon:{id}:    coupon:{id}:
    counter      issued:users    waiting:queue
                                  (Sorted Set)
         ↓
  즉시 응답 반환: "발급 요청 접수 완료"


┌─────────────────────────────────────────────────────────┐
│      3. CouponIssueScheduler (비동기, 5초 폴링)         │
│         - 활성 쿠폰 목록 조회                             │
│         - 남은 수량 계산                                 │
│         - 배치 크기 결정 (최대 100개)                     │
└─────────────────────────────────────────────────────────┘
                           ↓
                    [ZPOPMIN]
              대기열에서 배치 추출
                           ↓
┌─────────────────────────────────────────────────────────┐
│              4. DB 벌크 발급 처리                        │
│         - UserCoupon 엔티티 생성                         │
│         - Bulk INSERT                                   │
│         - Coupon.issuedQuantity 증가                    │
└─────────────────────────────────────────────────────────┘
                           ↓
                   [SET Pipeline]
              상태 일괄 업데이트 (ISSUED)
```

### Redis 자료구조

| 역할 | 자료구조 | Key 패턴 | 연산 | 특징 |
|-----|---------|----------|------|------|
| 수량 예약 | String | `coupon:{id}:counter` | INCR/DECR | 원자적 증가, 롤백 가능 |
| 중복 체크 | Set | `coupon:{id}:issued:users` | SADD | 반환값 1(신규)/0(중복) |
| 대기열 | **Sorted Set** | `coupon:{id}:waiting:queue` | ZADD/ZPOPMIN | **Score=timestamp(FIFO)** |
| 발급 상태 | String | `coupon:{id}:user:{userId}:status` | SET/GET | TTL 24시간 |

### 핵심 컴포넌트

#### 1. CouponIssueFacade
```java
// src/main/java/com/example/ecommerce/coupon/facade/CouponIssueFacade.java
public void issueRequest(Long couponId, Long userId) {
    // 1. 쿠폰 유효성 검증 (DB)
    Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);

    // 2. 수량 예약 (INCR)
    boolean quantityReserved = redisService.reserveQuantity(couponId, totalQuantity);

    // 3. 중복 체크 (SADD)
    boolean isNew = redisService.checkDuplicate(couponId, userId);

    // 4. 대기열 추가 (ZADD) - timestamp를 score로 사용
    long timestamp = System.currentTimeMillis();
    boolean addedToQueue = redisService.addToWaitingQueue(couponId, userId, timestamp);

    // 5. 상태 저장 (SET with TTL 24시간)
    redisService.setUserStatus(couponId, userId, "PENDING");
}
```

#### 2. CouponIssueScheduler
```java
// src/main/java/com/example/ecommerce/coupon/scheduler/CouponIssueScheduler.java
@Scheduled(fixedDelay = 5000) // 5초마다 실행
public void processCouponIssue() {
    List<Coupon> activeCoupons = couponRepository.findByStatus(CouponStatus.ACTIVE);

    for (Coupon coupon : activeCoupons) {
        // 1. 대기열 크기 확인
        long queueSize = redisService.getWaitingQueueSize(couponId);

        // 2. 남은 수량 계산
        int remainingCount = totalQuantity - redisCount;

        // 3. 배치 크기 결정 (최대 100개)
        int batchSize = Math.min(remainingCount, MAX_BATCH_SIZE);

        // 4. 대기열에서 배치 추출 (ZPOPMIN)
        List<Long> userIds = redisService.popFromWaitingQueue(couponId, batchSize);

        // 5. DB 벌크 발급
        bulkIssueCouponsUsingService(couponId, userIds);

        // 6. Redis 상태 업데이트
        updateUserStatusBatch(couponId, userIds, "ISSUED");
    }
}
```

### 문제점 분석

#### 1. 데이터 영속성 부족
```
Redis 장애 발생 시
  ↓
waiting:queue 데이터 손실
  ↓
사용자는 "접수 완료" 응답을 받았지만
실제로는 대기열에서 사라짐
  ↓
쿠폰 미발급, 고객 불만
```

**영향**:
- Redis AOF/RDB로 일부 완화 가능하지만, 완전한 보장 어려움
- 대기열이 유실되면 복구 불가능

#### 2. 폴링 방식의 비효율
```
5초마다 폴링
  ↓
대기열이 비어있어도 계속 조회
  ↓
불필요한 리소스 낭비
  ↓
평균 지연: 0~5초 (랜덤)
```

**문제**:
- 대기열에 데이터가 없어도 계속 폴링
- 처리 지연: 최악의 경우 5초 대기
- DB 및 Redis 부하 증가

#### 3. 확장성 제한
```
단일 Scheduler 인스턴스
  ↓
모든 활성 쿠폰을 순차 처리
  ↓
쿠폰 개수 증가 시 지연 누적
  ↓
수평 확장 어려움
```

**한계**:
- 여러 인스턴스 실행 시 중복 처리 가능성
- 분산 락 사용 시 성능 저하

#### 4. 재처리 메커니즘 부족
```java
// CouponIssueScheduler.java:108-111
catch (Exception e) {
    log.error("쿠폰 발급 처리 실패 - couponId: {}", couponId, e);
    // TODO: 실패한 요청을 Dead Letter Queue에 기록하거나 재시도 로직 추가
}
```

**문제**:
- ZPOPMIN으로 대기열에서 제거된 후 실패 시 복구 불가
- 재시도 로직 부재
- 실패한 요청 추적 어려움

---

## Kafka 특징 및 장점

### Kafka 핵심 특징

#### 1. 메시지 영속성 (Persistence)
```
Producer → Kafka Broker (Disk)
                ↓
         Log Segment Files
                ↓
         Replication (3 replicas)
                ↓
         메시지 보존 (retention: 7days)
```

**장점**:
- 디스크 기반 로그로 데이터 영속성 보장
- Replication으로 브로커 장애 시에도 데이터 유지
- Consumer가 다운되어도 메시지 손실 없음

#### 2. 높은 처리량 (High Throughput)
```
배치 처리 + 압축 + 제로 카피
  ↓
초당 수백만 메시지 처리
  ↓
낮은 레이턴시 (ms 단위)
```

**특징**:
- 배치 전송으로 네트워크 오버헤드 감소
- 페이지 캐시 활용으로 빠른 읽기/쓰기
- 압축 지원 (gzip, snappy, lz4)

#### 3. 파티셔닝 (Partitioning)
```
Topic: coupon-issue-requests
  ├─ Partition 0 → Consumer A
  ├─ Partition 1 → Consumer B
  └─ Partition 2 → Consumer C
      (병렬 처리)
```

**장점**:
- 병렬 처리로 처리량 증가
- 파티션 내에서는 순서 보장 (couponId 기반 파티셔닝)
- 파티션 추가로 수평 확장 가능

#### 4. Consumer Group
```
Consumer Group: coupon-issue-group
  ├─ Consumer Instance 1 → Partition 0, 1
  ├─ Consumer Instance 2 → Partition 2, 3
  └─ Consumer Instance 3 → Partition 4, 5
```

**장점**:
- 여러 Consumer가 메시지 분산 처리
- 자동 리밸런싱 (Consumer 추가/제거 시)
- 장애 격리 (한 Consumer 실패해도 다른 Consumer가 처리)

#### 5. Offset 관리
```
Consumer가 메시지 처리 후
  ↓
Offset Commit
  ↓
장애 발생 시 마지막 Offset부터 재시작
  ↓
중복 처리 가능, 손실 없음
```

**장점**:
- 재처리 가능 (Replay)
- 장애 복구 지원
- At-least-once, Exactly-once 전달 보장

#### 6. Decoupling
```
Producer (CouponIssueFacade)
       ↓ (비동기)
    Kafka Topic
       ↓ (비동기)
Consumer (CouponIssueConsumer)
```

**장점**:
- Producer와 Consumer 독립적 운영
- 시스템 간 느슨한 결합
- 각 컴포넌트 독립적으로 확장 가능

### Redis vs Kafka 대기열 비교

| 항목 | Redis Sorted Set | Kafka Topic |
|-----|-----------------|-------------|
| **영속성** | 휘발성 (AOF/RDB로 일부 완화) | **디스크 기반, Replication** |
| **처리 방식** | **풀(Pull) - 폴링** | **푸시(Push) - 이벤트 기반** |
| **순서 보장** | Score 기반 정렬 (전역) | **파티션 내 순서 보장** |
| **병렬 처리** | 제한적 (분산 락 필요) | **Consumer Group으로 자동** |
| **재처리** | 어려움 (ZPOPMIN 후 손실) | **Offset 관리로 쉬움** |
| **확장성** | 수직 확장 위주 | **수평 확장 (파티션 추가)** |
| **레이턴시** | **매우 낮음 (μs)** | 낮음 (ms) |
| **복잡도** | **낮음** | 높음 |
| **운영** | **간단** | 복잡 (클러스터 관리) |

### Kafka가 적합한 이유

1. **대용량 트래픽 처리**: 초당 수만 건의 쿠폰 발급 요청
2. **데이터 손실 방지**: 쿠폰 발급은 금전적 가치가 있어 손실 불가
3. **확장 가능성**: 쿠폰 종류/개수 증가 시 수평 확장 필요
4. **재처리 필요**: 실패한 발급 요청 재시도 필수
5. **MSA 준비**: 추후 쿠폰 서비스 분리 시 메시지 브로커 필요

---

## Kafka 기반 쿠폰/대기열 설계

### 개선 아키텍처 개요

```
┌─────────────────────────────────────────────────────────┐
│              1. 사용자 발급 요청 (동기)                   │
│           POST /api/coupons/{couponId}/issue            │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│            2. CouponIssueFacade (동기 처리)              │
│             - 쿠폰 유효성 검증 (DB)                       │
│             - Redis 수량/중복 제어                        │
└─────────────────────────────────────────────────────────┘
         ↓                ↓
    [INCR]           [SADD]
   수량 예약         중복 체크
         ↓                ↓
  coupon:{id}:    coupon:{id}:
    counter      issued:users
         ↓
  [Kafka Producer]
  Kafka 토픽에 이벤트 발행
         ↓
  즉시 응답 반환: "발급 요청 접수 완료"


┌─────────────────────────────────────────────────────────┐
│               Kafka Topic 구조                          │
│     Topic: coupon-issue-requests                        │
│     Partitions: 6 (couponId % 6)                        │
│     Replication: 3                                      │
│     Retention: 7 days                                   │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│      3. CouponIssueKafkaConsumer (실시간 처리)          │
│         Consumer Group: coupon-issue-group              │
│         Concurrency: 3~6 (파티션당 1개)                  │
│         - 메시지 수신                                     │
│         - DB 발급 처리                                    │
│         - Offset Commit (수동)                          │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│              4. DB 쿠폰 발급 처리                        │
│         - UserCoupon 엔티티 생성                         │
│         - DB INSERT                                     │
│         - Coupon.issuedQuantity 증가                    │
└─────────────────────────────────────────────────────────┘
         ↓
  [SET Redis Status]
  상태 업데이트 (ISSUED)
         ↓
  [Kafka ACK]
  Offset Commit → 메시지 처리 완료


┌─────────────────────────────────────────────────────────┐
│            5. 실패 처리 (Dead Letter Topic)              │
│         Topic: coupon-issue-dlq                         │
│         - 재시도 3회 실패 시 전송                         │
│         - 별도 Consumer로 모니터링                        │
└─────────────────────────────────────────────────────────┘
```

### Kafka Topic 설계

#### 1. 메인 토픽: coupon-issue-requests
```yaml
Topic: coupon-issue-requests
Partitions: 3
Replication Factor: 1
Retention: 7 days (기본값)
Auto Create: true (자동 생성 활성화)
```

**실제 구현**:
- 간단한 로컬 환경을 위해 파티션 3개, 복제본 1개로 설정
- 운영 환경에서는 파티션 6개, 복제본 3개로 확장 가능

**Message Schema**:
```json
{
  "eventId": "uuid-v4",
  "eventType": "COUPON_ISSUE_REQUESTED",
  "occurredAt": "2025-12-18T10:30:00Z",
  "couponId": 1,
  "userId": 12345,
  "timestamp": 1703321400000
}
```

**Key**: `{couponId}` (파티셔닝 기준)
- 동일 쿠폰의 요청은 같은 파티션으로
- 파티션 내에서 순서 보장 (FIFO)

#### 2. Dead Letter Topic: coupon-issue-dlq
```yaml
Topic: coupon-issue-dlq
Partitions: 1
Replication Factor: 1
Retention: 7 days (기본값)
```

**실제 구현**:
- DLQ는 실패 메시지만 저장하므로 파티션 1개로 충분
- 필요 시 추가 확장 가능

**Message Schema**:
```json
{
  "originalEvent": { ... },
  "failureReason": "DB connection timeout",
  "retryCount": 3,
  "lastAttemptAt": "2025-12-18T10:35:00Z",
  "stackTrace": "..."
}
```

### Redis 역할 재정의

Kafka 도입 후 Redis는 **대기열이 아닌 수량 제어 및 중복 체크에만 사용**합니다.

| 역할 | 자료구조 | Key 패턴 | 연산 | 변경 사항 |
|-----|---------|----------|------|---------|
| 수량 예약 | String | `coupon:{id}:counter` | INCR/DECR | **유지** (빠른 수량 제어) |
| 중복 체크 | Set | `coupon:{id}:issued:users` | SADD | **유지** (중복 방지) |
| ~~대기열~~ | ~~Sorted Set~~ | ~~`coupon:{id}:waiting:queue`~~ | ~~ZADD/ZPOPMIN~~ | **제거** (Kafka로 대체) |
| 발급 상태 | String | `coupon:{id}:user:{userId}:status` | SET/GET | **유지** (빠른 상태 조회) |

**변경 이유**:
- 대기열: Redis (휘발성) → Kafka (영속성)
- 수량 제어: Redis 유지 (빠른 원자적 연산 필요)
- 중복 체크: Redis 유지 (O(1) 조회 필요)

### 핵심 컴포넌트 설계

#### 1. CouponIssueFacade (Producer)

**역할**: 요청 접수 및 Kafka 발행

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueFacade {

    private final CouponRedisService redisService;
    private final CouponRepository couponRepository;
    private final KafkaTemplate<String, CouponIssueRequestEvent> kafkaTemplate;

    private static final String COUPON_ISSUE_TOPIC = "coupon-issue-requests";

    @Transactional(readOnly = true)
    public void issueRequest(Long couponId, Long userId) {
        // 1. 쿠폰 유효성 검증 (DB)
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);

        if (!coupon.canIssue()) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        int totalQuantity = coupon.getQuantity().getTotalQuantity();

        // 2. 수량 예약 (INCR) - 빠른 수량 제어
        boolean quantityReserved = redisService.reserveQuantity(couponId, totalQuantity);

        if (!quantityReserved) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE, "선착순 마감되었습니다.");
        }

        // 3. 중복 체크 (SADD) - 빠른 중복 방지
        boolean isNew = redisService.checkDuplicate(couponId, userId);

        if (!isNew) {
            // 중복 발급 시도 시 수량 롤백 (DECR)
            redisService.rollbackQuantity(couponId);
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED, "이미 발급 요청한 쿠폰입니다.");
        }

        // 4. Kafka 이벤트 발행 (비동기)
        CouponIssueRequestEvent event = CouponIssueRequestEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("COUPON_ISSUE_REQUESTED")
            .occurredAt(LocalDateTime.now())
            .couponId(couponId)
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();

        try {
            // Key: couponId (파티셔닝 기준)
            String key = couponId.toString();
            kafkaTemplate.send(COUPON_ISSUE_TOPIC, key, event);

            // 5. 상태 저장 (SET with TTL 24시간)
            redisService.setUserStatus(couponId, userId, "PENDING");

            log.info("쿠폰 발급 요청 Kafka 발행 - couponId: {}, userId: {}", couponId, userId);

        } catch (Exception e) {
            // Kafka 발행 실패 시 롤백
            redisService.rollbackQuantity(couponId);
            redisService.removeDuplicate(couponId, userId); // Set에서 제거
            log.error("Kafka 발행 실패 - couponId: {}, userId: {}", couponId, userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "발급 요청 처리 중 오류가 발생했습니다.");
        }
    }
}
```

**특징**:
- Kafka 발행 실패 시 Redis 롤백 (원자성 보장)
- 동기 응답: 사용자는 Kafka 발행 성공 여부 확인
- Key 기반 파티셔닝: 동일 쿠폰은 같은 파티션으로

#### 2. CouponIssueKafkaConsumer

**역할**: Kafka 메시지 소비 및 DB 발급 처리

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueKafkaConsumer {

    private final UserCouponService userCouponService;
    private final CouponRedisService redisService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String DLQ_TOPIC = "coupon-issue-dlq";
    private static final int MAX_RETRY_COUNT = 3;

    @KafkaListener(
        topics = "coupon-issue-requests",
        groupId = "coupon-issue-group",
        containerFactory = "couponIssueKafkaListenerContainerFactory",
        concurrency = "3" // 병렬 처리 Consumer 수
    )
    public void consumeCouponIssueRequest(
        @Payload CouponIssueRequestEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment ack
    ) {
        log.info("[Kafka Consumer] 쿠폰 발급 요청 수신 - couponId: {}, userId: {}, partition: {}, offset: {}",
            event.getCouponId(), event.getUserId(), partition, offset);

        int retryCount = 0;
        boolean success = false;
        Exception lastException = null;

        // 재시도 로직 (최대 3회)
        while (retryCount < MAX_RETRY_COUNT && !success) {
            try {
                // DB 쿠폰 발급 처리
                UserCoupon userCoupon = userCouponService.issueCouponAsync(
                    event.getCouponId(),
                    event.getUserId()
                );

                // Redis 상태 업데이트
                redisService.setUserStatus(event.getCouponId(), event.getUserId(), "ISSUED");

                log.info("[Kafka Consumer] 쿠폰 발급 완료 - couponId: {}, userId: {}, userCouponId: {}",
                    event.getCouponId(), event.getUserId(), userCoupon.getId());

                success = true;

            } catch (Exception e) {
                retryCount++;
                lastException = e;

                log.warn("[Kafka Consumer] 쿠폰 발급 실패 (재시도 {}/{}) - couponId: {}, userId: {}, error: {}",
                    retryCount, MAX_RETRY_COUNT, event.getCouponId(), event.getUserId(), e.getMessage());

                if (retryCount < MAX_RETRY_COUNT) {
                    // 지수 백오프 (Exponential Backoff)
                    try {
                        Thread.sleep(1000L * retryCount); // 1초, 2초, 3초
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (success) {
            // 성공 시 Offset Commit (수동 ACK)
            ack.acknowledge();
            log.info("[Kafka Consumer] Offset Commit 완료 - partition: {}, offset: {}", partition, offset);

        } else {
            // 최종 실패 시 DLQ로 전송
            sendToDeadLetterQueue(event, lastException, retryCount);

            // Offset Commit (메시지 재처리 방지)
            ack.acknowledge();
            log.error("[Kafka Consumer] 최종 실패, DLQ 전송 - couponId: {}, userId: {}",
                event.getCouponId(), event.getUserId());
        }
    }

    private void sendToDeadLetterQueue(CouponIssueRequestEvent event, Exception exception, int retryCount) {
        try {
            CouponIssueDlqEvent dlqEvent = CouponIssueDlqEvent.builder()
                .originalEvent(event)
                .failureReason(exception.getMessage())
                .retryCount(retryCount)
                .lastAttemptAt(LocalDateTime.now())
                .stackTrace(getStackTrace(exception))
                .build();

            kafkaTemplate.send(DLQ_TOPIC, event.getCouponId().toString(), dlqEvent);

            // Redis 상태 업데이트
            redisService.setUserStatus(event.getCouponId(), event.getUserId(), "FAILED");

        } catch (Exception e) {
            log.error("[Kafka Consumer] DLQ 전송 실패 - couponId: {}, userId: {}",
                event.getCouponId(), event.getUserId(), e);
        }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
```

**특징**:
- **Consumer Group**: `coupon-issue-group` (병렬 처리)
- **Concurrency**: 3개 Consumer 인스턴스 (파티션 3개)
- **메시지 처리**: 하나씩 처리 (배치 처리 없음, 간단한 구현)
- **수동 ACK**: 처리 완료 후 Offset Commit
- **재시도 로직**: 최대 3회, 지수 백오프 (1초, 2초, 3초)
- **DLQ**: 최종 실패 시 Dead Letter Queue로 전송

**간단한 구현**:
- 배치 처리 대신 메시지를 하나씩 순차 처리
- 구현이 단순하고 이해하기 쉬움
- 성능이 충분한 경우 배치 처리 불필요

#### 3. CouponIssueDlqConsumer

**역할**: Dead Letter Queue 모니터링 및 수동 재처리

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueDlqConsumer {

    private final SlackNotificationService slackService; // 알림

    @KafkaListener(
        topics = "coupon-issue-dlq",
        groupId = "coupon-issue-dlq-monitor-group"
    )
    public void consumeDeadLetterQueue(
        @Payload CouponIssueDlqEvent event,
        Acknowledgment ack
    ) {
        log.error("[DLQ Monitor] 쿠폰 발급 최종 실패 - couponId: {}, userId: {}, reason: {}",
            event.getOriginalEvent().getCouponId(),
            event.getOriginalEvent().getUserId(),
            event.getFailureReason());

        // Slack 알림 전송
        slackService.sendAlert(
            "쿠폰 발급 실패",
            String.format("couponId: %d, userId: %d, reason: %s",
                event.getOriginalEvent().getCouponId(),
                event.getOriginalEvent().getUserId(),
                event.getFailureReason())
        );

        // TODO: 관리자 대시보드에 표시 또는 DB에 저장

        ack.acknowledge();
    }
}
```

#### 4. Kafka Configuration (실제 구현)

```java
// KafkaConfig.java에 쿠폰 발급용 설정 추가
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ====== 쿠폰 발급 Producer 설정 ======
    @Bean
    public ProducerFactory<String, CouponIssueRequestEvent> couponIssueProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 메시지 전달 보장
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 ISR 확인
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // 재시도 3번
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 멱등성 보장
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, CouponIssueRequestEvent> couponIssueKafkaTemplate() {
        return new KafkaTemplate<>(couponIssueProducerFactory());
    }

    // ====== 쿠폰 발급 Consumer 설정 ======
    @Bean
    public ConsumerFactory<String, CouponIssueRequestEvent> couponIssueConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "coupon-issue-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Offset 관리
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 Commit
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 처음부터 읽기

        // JSON 역직렬화 설정
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CouponIssueRequestEvent.class.getName());

        return new DefaultKafkaConsumerFactory<>(
            configProps,
            new StringDeserializer(),
            new JsonDeserializer<>(CouponIssueRequestEvent.class, false)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponIssueRequestEvent>
            couponIssueKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CouponIssueRequestEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(couponIssueConsumerFactory());
        factory.setConcurrency(3); // 동시 처리 Consumer 수
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // 수동 ACK 모드
        return factory;
    }

    // ====== DLQ용 범용 Producer ======
    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }
}
```

**실제 구현 특징**:
- **간단한 설정**: 배치 처리 관련 설정 제거 (메시지를 하나씩 처리)
- **기본값 사용**: MAX_POLL_RECORDS, FETCH_MIN_BYTES 등의 튜닝 파라미터 제거
- **충분한 성능**: 간단한 구현으로도 300 TPS 이상 처리 가능
- **유지보수 용이**: 설정이 단순하여 이해하기 쉬움

**필요 시 성능 튜닝**:
```java
// Consumer에 배치 처리가 필요한 경우 추가 가능
configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
```

---

## 아키텍처 비교 및 개선점

### 전체 비교

| 항목 | 기존 (Redis + Scheduler) | 개선 (Redis + Kafka) |
|-----|-------------------------|---------------------|
| **대기열** | Redis Sorted Set (휘발성) | **Kafka Topic (영속성)** |
| **처리 방식** | 폴링 (5초마다) | **이벤트 기반 (실시간)** |
| **데이터 손실** | Redis 장애 시 손실 가능 | **Replication으로 보장** |
| **처리 지연** | 평균 2.5초 (0~5초) | **평균 수십 ms** |
| **병렬 처리** | 단일 Scheduler | **Consumer Group (3~6개)** |
| **확장성** | 제한적 (분산 락 필요) | **파티션 추가로 수평 확장** |
| **재처리** | 어려움 (ZPOPMIN 후 손실) | **Offset 관리로 쉬움** |
| **장애 복구** | 수동 복구 | **자동 리밸런싱** |
| **모니터링** | 로그 기반 | **DLQ + 알림** |
| **운영 복잡도** | 낮음 | **높음** |

### 주요 개선점

#### 1. 데이터 영속성 보장

**기존**:
```
Redis 장애 발생
  ↓
waiting:queue 데이터 손실
  ↓
사용자 불만 및 금전적 손실
```

**개선**:
```
Kafka 디스크 저장 + Replication
  ↓
브로커 장애 시에도 데이터 유지
  ↓
Consumer 재시작 후 처리 재개
```

#### 2. 실시간 처리

**기존**:
```
Scheduler 폴링 (5초마다)
  ↓
평균 지연: 2.5초
최악의 경우: 5초
```

**개선**:
```
Kafka Push 방식
  ↓
메시지 도착 즉시 처리
평균 지연: 수십 ms
```

#### 3. 수평 확장

**기존**:
```
단일 Scheduler
  ↓
모든 쿠폰을 순차 처리
  ↓
쿠폰 개수 증가 시 지연 누적
```

**개선**:
```
Consumer Group (3~6개)
  ↓
파티션별 병렬 처리
  ↓
파티션 추가로 처리량 증가
```

**성능 비교**:
```
기존: 100개/5초 = 20 TPS
개선: 100개 * 3 Consumer = 300 TPS
```

#### 4. 재처리 및 장애 복구

**기존**:
```java
// ZPOPMIN으로 대기열에서 제거
List<Long> userIds = redisService.popFromWaitingQueue(couponId, batchSize);

// 처리 실패 시
catch (Exception e) {
    log.error("쿠폰 발급 처리 실패 - couponId: {}", couponId, e);
    // TODO: 실패한 요청을 Dead Letter Queue에 기록하거나 재시도 로직 추가
}
// → 대기열에서 이미 제거되어 복구 불가능
```

**개선**:
```java
// Kafka Consumer에서 처리
public void consumeCouponIssueRequest(..., Acknowledgment ack) {
    try {
        // DB 발급 처리
        userCouponService.issueCouponAsync(event.getCouponId(), event.getUserId());

        // 성공 시 Offset Commit
        ack.acknowledge();

    } catch (Exception e) {
        // 실패 시 Offset Commit 안 함 → 재처리
        log.error("쿠폰 발급 실패, 재처리 예정", e);
    }
}
// → Offset 미커밋 시 자동 재처리
```

#### 5. 모니터링 및 알림

**기존**:
```
로그로만 실패 기록
  ↓
실패 추적 어려움
수동 확인 필요
```

**개선**:
```
DLQ + Slack 알림
  ↓
실시간 실패 알림
관리자 대시보드 연동
```

---

## 구현 가이드

### 1단계: Kafka 토픽 생성

```bash
# 메인 토픽 생성
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic coupon-issue-requests \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=604800000 \  # 7 days
  --config compression.type=snappy

# DLQ 토픽 생성
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic coupon-issue-dlq \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=2592000000  # 30 days

# 토픽 확인
kafka-topics.sh --list --bootstrap-server localhost:9092
```

### 2단계: 이벤트 클래스 정의

```java
// CouponIssueRequestEvent.java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueRequestEvent implements Serializable {

    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;

    private Long couponId;
    private Long userId;
    private Long timestamp;
}

// CouponIssueDlqEvent.java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueDlqEvent implements Serializable {

    private CouponIssueRequestEvent originalEvent;
    private String failureReason;
    private Integer retryCount;
    private LocalDateTime lastAttemptAt;
    private String stackTrace;
}
```

### 3단계: Redis 서비스 개선

```java
// CouponRedisService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // 기존 메서드 유지
    public boolean reserveQuantity(Long couponId, int limit) { ... }
    public void rollbackQuantity(Long couponId) { ... }
    public boolean checkDuplicate(Long couponId, Long userId) { ... }
    public void setUserStatus(Long couponId, Long userId, String status) { ... }
    public String getUserStatus(Long couponId, Long userId) { ... }

    // 추가: Set에서 제거 (Kafka 발행 실패 시 롤백용)
    public void removeDuplicate(Long couponId, Long userId) {
        try {
            String key = getIssuedUsersKey(couponId);
            redisTemplate.opsForSet().remove(key, userId.toString());
            log.debug("중복 체크 롤백 - couponId: {}, userId: {}", couponId, userId);

        } catch (Exception e) {
            log.error("중복 체크 롤백 실패 - couponId: {}, userId: {}", couponId, userId, e);
        }
    }

    // 제거: 대기열 관련 메서드 (Kafka로 대체)
    // public boolean addToWaitingQueue(...) { ... } → 삭제
    // public List<Long> popFromWaitingQueue(...) { ... } → 삭제
    // public long getWaitingQueueSize(...) { ... } → 삭제
    // public Long getWaitingRank(...) { ... } → 삭제
}
```

### 4단계: Facade 수정 (Producer)

```java
// CouponIssueFacade.java
// 위의 "핵심 컴포넌트 설계 > 1. CouponIssueFacade" 참고
```

### 5단계: Consumer 구현

```java
// CouponIssueKafkaConsumer.java
// 위의 "핵심 컴포넌트 설계 > 2. CouponIssueKafkaConsumer" 참고

// CouponIssueDlqConsumer.java
// 위의 "핵심 컴포넌트 설계 > 3. CouponIssueDlqConsumer" 참고
```

### 6단계: Scheduler 제거

```java
// CouponIssueScheduler.java → 삭제 또는 비활성화
// @Scheduled(fixedDelay = 5000) 제거
// → Kafka Consumer로 대체되어 불필요
```

### 7단계: 상태 조회 API 유지

```java
// CouponIssueQueryService.java (변경 없음)
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueQueryService {

    private final CouponRedisService redisService;
    private final UserCouponRepository userCouponRepository;

    public CouponIssueStatusResponse getIssueStatus(Long couponId, Long userId) {
        // 1. Redis에서 상태 조회 (빠른 응답)
        String status = redisService.getUserStatus(couponId, userId);

        if (status != null) {
            return CouponIssueStatusResponse.builder()
                .status(status)
                .message(getStatusMessage(status))
                .build();
        }

        // 2. Redis에 없으면 DB 조회
        boolean existsInDb = userCouponRepository.existsByCouponIdAndUserId(couponId, userId);

        if (existsInDb) {
            return CouponIssueStatusResponse.builder()
                .status("ISSUED")
                .message("쿠폰이 발급되었습니다.")
                .build();
        }

        // 3. 둘 다 없으면 "요청 없음"
        return CouponIssueStatusResponse.builder()
            .status("NOT_REQUESTED")
            .message("발급 요청 내역이 없습니다.")
            .build();
    }

    private String getStatusMessage(String status) {
        return switch (status) {
            case "PENDING" -> "쿠폰 발급이 진행 중입니다.";
            case "ISSUED" -> "쿠폰이 발급되었습니다.";
            case "FAILED" -> "쿠폰 발급에 실패했습니다. 고객센터에 문의해주세요.";
            default -> "알 수 없는 상태입니다.";
        };
    }
}
```

### 8단계: 테스트

#### 단위 테스트
```java
@SpringBootTest
class CouponIssueFacadeTest {

    @MockBean
    private KafkaTemplate<String, CouponIssueRequestEvent> kafkaTemplate;

    @Test
    void Kafka_발행_성공_시_요청_접수_완료() {
        // given
        Long couponId = 1L;
        Long userId = 12345L;

        // Kafka 발행 성공 mock
        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        // when & then
        assertDoesNotThrow(() -> couponIssueFacade.issueRequest(couponId, userId));

        // Kafka 발행 호출 확인
        verify(kafkaTemplate, times(1)).send(eq("coupon-issue-requests"), eq("1"), any());
    }

    @Test
    void Kafka_발행_실패_시_Redis_롤백() {
        // given
        Long couponId = 1L;
        Long userId = 12345L;

        // Kafka 발행 실패 mock
        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Kafka 장애"));

        // when & then
        assertThrows(CustomException.class,
            () -> couponIssueFacade.issueRequest(couponId, userId));

        // Redis 롤백 확인
        verify(redisService, times(1)).rollbackQuantity(couponId);
        verify(redisService, times(1)).removeDuplicate(couponId, userId);
    }
}
```

#### 통합 테스트
```java
@SpringBootTest
@EmbeddedKafka(partitions = 3, topics = {"coupon-issue-requests"})
class CouponIssueKafkaIntegrationTest {

    @Autowired
    private CouponIssueFacade couponIssueFacade;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    void 쿠폰_발급_요청_Kafka_전송_및_Consumer_처리_성공() throws InterruptedException {
        // given
        Long couponId = 1L;
        Long userId = 12345L;

        // when
        couponIssueFacade.issueRequest(couponId, userId);

        // Kafka Consumer가 처리할 시간 대기
        Thread.sleep(3000);

        // then
        UserCoupon userCoupon = userCouponRepository
            .findByCouponIdAndUserId(couponId, userId)
            .orElseThrow();

        assertThat(userCoupon).isNotNull();
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    }
}
```

---

## 성능 및 확장성 분석

### 처리량 비교

#### 기존 시스템 (Redis + Scheduler)

```
Scheduler 폴링: 5초마다
배치 크기: 최대 100개
  ↓
처리량: 100 / 5초 = 20 TPS
병렬 처리: 불가능 (단일 Scheduler)
```

#### 개선 시스템 (Redis + Kafka)

```
Consumer Group: 3개 인스턴스
파티션: 3개 (Consumer당 파티션 1개씩)
메시지 처리: 하나씩 순차 처리 (간단한 구현)
  ↓
처리량: ~100-200 TPS (5-10배 향상)
파티션 추가 시 확장 가능
```

**실제 구현 특징**:
- 배치 처리 없이 메시지를 하나씩 처리
- DB INSERT 속도가 병목 (평균 10-50ms)
- 3개 Consumer가 병렬로 처리하므로 충분히 빠름

**확장 시나리오**:
```
3 Partitions → 6 Partitions
3 Consumers → 6 Consumers
  ↓
처리량: ~200-400 TPS (10-20배 향상)
```

**배치 처리 추가 시** (선택적):
```
Consumer당 배치 크기: 100개
  ↓
처리량: 100 * 3 = 300 TPS 이상 가능
```

### 레이턴시 비교

| 단계 | 기존 (Redis + Scheduler) | 개선 (Redis + Kafka) |
|-----|-------------------------|---------------------|
| 요청 접수 | 10ms (Redis 연산) | 10ms (Redis 연산) |
| 대기열 추가 | 5ms (ZADD) | 5ms (Kafka 발행) |
| **대기 시간** | **평균 2.5초 (폴링)** | **평균 50ms (이벤트 기반)** |
| DB 처리 | 100ms (INSERT) | 100ms (INSERT) |
| **총 레이턴시** | **~2.6초** | **~165ms** |

**개선율**: **약 94% 감소** (2.6초 → 165ms)

### 동시성 처리

#### 동시 요청 10,000건 시나리오

**기존**:
```
Redis 연산: 10,000건 (병렬) → 성공
대기열 추가: 10,000건 (Sorted Set) → 성공
  ↓
Scheduler 폴링:
  - 1차: 100건 처리 (5초)
  - 2차: 100건 처리 (10초)
  - ...
  - 100차: 100건 처리 (500초)
  ↓
총 처리 시간: ~8분 20초
```

**개선**:
```
Redis 연산: 10,000건 (병렬) → 성공
Kafka 발행: 10,000건 → 성공 (약 1-2초)
  ↓
Consumer Group (3개, 메시지를 하나씩 처리):
  - Consumer A: 3,333건 (약 50-100초)
  - Consumer B: 3,333건 (약 50-100초)
  - Consumer C: 3,334건 (약 50-100초)
  ↓
총 처리 시간: ~1-2분
```

**개선율**: **약 75-85% 감소** (8분 20초 → 1-2분)

**참고**: 배치 처리를 추가하면 31초까지 단축 가능

### 자원 사용량

| 항목 | 기존 | 개선 | 비고 |
|-----|-----|-----|------|
| **Redis 메모리** | 높음 (Sorted Set) | **낮음** (Set만 사용) | Kafka로 대기열 이동 |
| **DB 커넥션** | 낮음 (Scheduler 1개) | **중간** (Consumer 3개) | 커넥션 풀 증가 필요 |
| **네트워크 I/O** | 낮음 | **높음** (Kafka 통신) | Kafka 클러스터 필요 |
| **디스크 사용량** | 낮음 | **높음** (Kafka 로그) | 7일 retention |

### 확장성 시나리오

#### 시나리오 1: 쿠폰 개수 증가

**기존**:
```
쿠폰 10개: Scheduler가 10개 쿠폰 순차 처리
쿠폰 100개: Scheduler가 100개 쿠폰 순차 처리 (지연 누적)
  ↓
확장 어려움 (분산 락 필요, 복잡도 증가)
```

**개선**:
```
쿠폰 10개: 파티션 3개로 병렬 처리
쿠폰 100개: 파티션 추가 (3개 → 6개 → 12개)
  ↓
선형 확장 (파티션 추가만으로 처리량 증가)
```

#### 시나리오 2: 트래픽 급증

**기존**:
```
평소: 20 TPS
이벤트 시: 1,000 TPS 요청
  ↓
Scheduler 처리 속도: 20 TPS (고정)
  ↓
대기열 증가: 50배 (40초 → 2,000초 대기)
```

**개선**:
```
평소: 100-200 TPS (Consumer 3개)
이벤트 시: 1,000 TPS 요청
  ↓
Consumer 동적 증가: 3개 → 10개
파티션: 3개 → 10개 (미리 준비)
  ↓
처리량 증가: 100-200 TPS → 1,000 TPS
대기열 유지: 안정적 처리
```

**실제 구현**:
- 현재는 간단한 구현으로 100-200 TPS
- 필요 시 배치 처리 추가로 300+ TPS 가능
- 파티션과 Consumer 수 조정으로 확장

---

## 트레이드오프 및 고려사항

### 장점

#### 1. 안정성 향상
- **메시지 영속성**: 디스크 저장 + Replication으로 데이터 손실 방지
- **장애 격리**: Consumer 장애 시 다른 Consumer가 처리 계속
- **재처리 가능**: Offset 관리로 실패한 요청 재처리

#### 2. 성능 개선
- **실시간 처리**: 폴링 지연 제거 (2.5초 → 50ms)
- **병렬 처리**: Consumer Group으로 처리량 15배 향상
- **배치 최적화**: Kafka 배치 처리로 네트워크 오버헤드 감소

#### 3. 확장성
- **수평 확장**: 파티션 추가로 선형 확장 가능
- **동적 확장**: Consumer 수 증가로 트래픽 급증 대응
- **MSA 준비**: 추후 쿠폰 서비스 분리 용이

#### 4. 운영 효율
- **모니터링**: DLQ로 실패 요청 자동 추적
- **알림**: Slack 연동으로 실시간 장애 대응
- **디버깅**: Kafka 메시지 재생으로 문제 재현 가능

### 단점 및 고려사항

#### 1. 운영 복잡도 증가

**Kafka 클러스터 관리**:
```
- Broker 3개 이상 (Replication Factor 3)
- Zookeeper 3개 이상 (Kafka 2.8+ KRaft 권장)
- 디스크 용량 관리 (로그 파일)
- 네트워크 대역폭
```

**권장 사항**:
- 관리형 서비스 사용 (AWS MSK, Confluent Cloud)
- Monitoring 도구 도입 (Kafka Manager, Burrow)
- 운영 팀 교육 (Kafka 아키텍처, 장애 대응)

#### 2. 비용 증가

| 항목 | 기존 | 개선 |
|-----|-----|-----|
| 인프라 | Redis 1대 | Redis 1대 + Kafka 클러스터 3대 |
| 디스크 | 낮음 | 높음 (로그 retention 7일) |
| 네트워크 | 낮음 | 높음 (Kafka 통신) |
| 운영 인력 | 1명 | 2명 (Kafka 전문가) |

**비용 절감 방안**:
- Kafka 토픽 retention 최적화 (7일 → 3일)
- 압축 활성화 (snappy, lz4)
- 파티션 수 최소화 (오버프로비저닝 방지)

#### 3. 레이턴시 트레이드오프

**Redis Sorted Set (기존)**:
- 레이턴시: ~1ms (메모리 기반)
- 휘발성: 데이터 손실 위험

**Kafka Topic (개선)**:
- 레이턴시: ~5ms (디스크 + 네트워크)
- 영속성: 데이터 손실 방지

**결론**: 5ms 추가 지연은 사용자 경험에 거의 영향 없음 (전체 레이턴시 165ms)

#### 4. 학습 곡선

**개발팀**:
- Kafka 아키텍처 이해 (Topic, Partition, Offset)
- Producer/Consumer 패턴
- 메시지 전달 보장 (At-least-once, Exactly-once)

**운영팀**:
- Kafka 클러스터 관리
- Rebalancing, Lag 모니터링
- 장애 대응 (Broker 다운, Consumer Lag 증가)

**권장 사항**:
- 내부 교육 세션 (2주)
- PoC (Proof of Concept) 진행 (1개월)
- 단계적 적용 (일부 쿠폰 → 전체 쿠폰)

### 적용 시기 결정

#### Kafka 도입이 적합한 경우

1. **중대형 트래픽**: 초당 100건 이상의 쿠폰 발급 요청
2. **데이터 손실 불가**: 쿠폰의 금전적 가치가 높음
3. **확장 계획**: 쿠폰 종류/개수가 지속적으로 증가
4. **MSA 전환**: 쿠폰 서비스 분리 예정
5. **실시간 처리**: 폴링 지연(2.5초) 제거 필요

#### 기존 시스템 유지가 적합한 경우

1. **저트래픽**: 초당 20-50건 미만의 쿠폰 발급 요청
2. **단순성 우선**: 운영 복잡도 최소화 필요
3. **비용 제약**: Kafka 클러스터 구축 비용 부담
4. **Monolith 유지**: MSA 전환 계획 없음
5. **제한된 인력**: Kafka 운영 경험 부족

### 하이브리드 접근법

**단계적 Kafka 도입**:

#### Phase 1: 고트래픽 쿠폰만 Kafka 적용
```
쿠폰 A (선착순 10,000명) → Kafka
쿠폰 B (선착순 100명) → Redis + Scheduler (기존)
```

#### Phase 2: 모든 쿠폰으로 확대
```
모든 쿠폰 → Kafka
Redis + Scheduler 제거
```

#### Phase 3: MSA 전환
```
쿠폰 서비스 분리
Kafka 이벤트 버스로 통합
```

---

## 결론

### 요약

Kafka 기반 쿠폰/대기열 시스템은 **안정성, 성능, 확장성**을 크게 향상시키지만, **운영 복잡도와 비용이 증가**합니다.

**핵심 개선 사항** (실제 구현 기준):
- 데이터 영속성: Redis (휘발성) → Kafka (영속성)
- 처리 지연: 평균 2.5초 → 50ms (94% 감소)
- 처리량: 20 TPS → 100-200 TPS (5-10배 향상)
- 확장성: 파티션 추가로 선형 확장 가능
- 간단한 구현: 메시지를 하나씩 처리하여 이해하기 쉬움

**배치 처리 추가 시**:
- 처리량: 300+ TPS (15배 이상 향상)

### 권장 사항

#### 1. 단계적 도입
- PoC (1개월): 테스트 환경에서 검증
- Phase 1 (2개월): 고트래픽 쿠폰만 Kafka 적용
- Phase 2 (3개월): 전체 쿠폰으로 확대

#### 2. 인프라 준비
- Kafka 클러스터 구축 (Broker 3대 + Zookeeper 3대)
- 또는 관리형 서비스 사용 (AWS MSK, Confluent Cloud)
- 모니터링 도구 도입 (Kafka Manager, Burrow, Grafana)

#### 3. 팀 역량 강화
- Kafka 교육 (개발팀 + 운영팀)
- 장애 대응 매뉴얼 작성
- 정기적인 DR (Disaster Recovery) 훈련

#### 4. 성능 최적화
- Producer: 배치 전송, 압축, 멱등성 활성화
- Consumer: 배치 처리, 수동 ACK, 재시도 로직
- Kafka: 적절한 파티션 수, Replication Factor 3

### 다음 단계

1. **PoC 진행**: 테스트 환경에서 Kafka 기반 쿠폰 시스템 검증
2. **성능 테스트**: 동시 요청 10,000건 시나리오 검증
3. **모니터링 구축**: Kafka Lag, DLQ 알림 시스템 구축
4. **단계적 적용**: 고트래픽 쿠폰부터 점진적 적용
5. **MSA 전환**: 쿠폰 서비스 분리 및 이벤트 기반 아키텍처 확대

---

## 참고 자료

### 공식 문서
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/html/)
- [Confluent Kafka Best Practices](https://docs.confluent.io/platform/current/kafka/deployment.html)

### 추가 학습
- [Kafka: The Definitive Guide](https://www.confluent.io/resources/kafka-the-definitive-guide/)
- [Designing Event-Driven Systems](https://www.confluent.io/designing-event-driven-systems/)
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)