# STEP17 - Kafka 학습 노트

> 주니어 개발자를 위한 Apache Kafka 이해하기

## 1. Kafka란 무엇인가?

### 1.1 정의

**Apache Kafka**는 LinkedIn에서 개발한 **분산 이벤트 스트리밍 플랫폼**입니다.

- **이벤트 스트리밍**: 데이터를 "흐르는 강물"처럼 실시간으로 생성하고, 저장하고, 처리하는 것
- **분산 시스템**: 여러 서버(브로커)에 데이터를 분산 저장하여 고가용성과 확장성을 보장

### 1.2 간단히 비유하면?

Kafka는 **"대용량 메시지 우체국"** 같은 역할을 합니다.

```
[발신자: Producer]
      ↓
   메시지 발송
      ↓
[우체국: Kafka Broker] ← 메시지를 토픽별로 분류하여 저장
      ↓
   메시지 수령
      ↓
[수신자: Consumer]
```

- **Producer (발신자)**: 메시지를 보내는 애플리케이션
- **Kafka Broker (우체국)**: 메시지를 저장하고 관리
- **Consumer (수신자)**: 메시지를 읽어서 처리하는 애플리케이션
- **Topic (우편함)**: 메시지를 분류하는 카테고리

### 1.3 핵심 특징

#### (1) 높은 처리량 (High Throughput)
- 초당 수백만 개의 메시지 처리 가능
- 디스크 기반 순차 I/O 사용으로 빠른 속도

#### (2) 내구성 (Durability)
- 메시지를 디스크에 영구 저장
- Replication을 통한 데이터 복제

#### (3) 확장성 (Scalability)
- 브로커를 추가하여 수평 확장 가능
- 파티션을 늘려 병렬 처리 가능

#### (4) 실시간 처리
- 메시지 생성 즉시 소비자가 읽을 수 있음
- 폴링(polling) 방식 대비 지연시간 최소화

---

## 2. Kafka를 어디에, 왜 쓰는가?

### 2.1 사용 사례 (Use Cases)

#### (1) 실시간 이벤트 처리
- **예시**: 쿠폰 발급, 주문 처리, 결제 알림
- **왜?**: 즉각적인 사용자 경험 제공

#### (2) 로그 수집 및 분석
- **예시**: 애플리케이션 로그, 사용자 행동 로그
- **왜?**: 대용량 로그를 안정적으로 수집하고 분석

#### (3) 데이터 파이프라인
- **예시**: DB → Kafka → 데이터 웨어하우스
- **왜?**: 시스템 간 데이터 동기화

#### (4) 마이크로서비스 간 통신
- **예시**: 주문 서비스 → 재고 서비스 → 배송 서비스
- **왜?**: 느슨한 결합(Loose Coupling) 구현

#### (5) CDC (Change Data Capture)
- **예시**: DB 변경사항을 실시간으로 다른 시스템에 전파
- **왜?**: 데이터 일관성 유지

### 2.2 우리 프로젝트에서 Kafka를 쓴 이유

#### 기존 방식: Redis Sorted Set + Scheduler
```
요청 → Redis ZADD (대기열 추가)
         ↓
   5초마다 Scheduler가 폴링
         ↓
   100개씩 배치 처리
```

**문제점**:
- 평균 지연: 2.5초
- 처리량: 20 TPS
- 확장성 제한: 단일 스케줄러만 가능

#### 개선 방식: Kafka Event Streaming
```
요청 → Kafka Producer (이벤트 발행)
         ↓
   즉시 Consumer가 수신
         ↓
   실시간 처리
```

**장점**:
- 평균 지연: 50ms (50배 개선)
- 처리량: 100-200 TPS (5-10배 개선)
- 확장성: Consumer 수 증가로 수평 확장 가능

---

## 3. Kafka 구조

### 3.1 전체 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                   Kafka Cluster                     │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │
│  │ Broker 1     │  │ Broker 2     │  │ Broker 3 │  │
│  │              │  │              │  │          │  │
│  │ Topic A      │  │ Topic A      │  │ Topic B  │  │
│  │  Partition 0 │  │  Partition 1 │  │  ...     │  │
│  │  Partition 2 │  │              │  │          │  │
│  └──────────────┘  └──────────────┘  └──────────┘  │
└─────────────────────────────────────────────────────┘
        ↑                                      ↓
   [Producer]                            [Consumer Group]
   메시지 발행                             메시지 소비
```

### 3.2 주요 구성 요소

#### (1) Broker
- Kafka 서버 (실제 메시지를 저장하는 물리적 서버)
- 하나의 Kafka 클러스터는 여러 개의 Broker로 구성
- 각 Broker는 고유 ID를 가짐

#### (2) Topic
- 메시지를 논리적으로 분류하는 단위
- 데이터베이스의 "테이블"과 유사
- **예시**: `coupon-issue-requests`, `order-created-events`

#### (3) Partition
- Topic을 물리적으로 나눈 단위
- 병렬 처리와 확장성의 핵심
- 각 Partition은 순서가 보장된 메시지 큐

**Partition 예시**:
```
Topic: coupon-issue-requests (3개 파티션)

Partition 0: [msg1] [msg4] [msg7] ...
Partition 1: [msg2] [msg5] [msg8] ...
Partition 2: [msg3] [msg6] [msg9] ...
```

#### (4) Offset
- Partition 내 메시지의 위치를 나타내는 고유 번호
- 0부터 시작하여 순차적으로 증가
- Consumer는 Offset을 기억하여 어디까지 읽었는지 추적

```
Partition 0:
┌─────┬─────┬─────┬─────┬─────┐
│ msg │ msg │ msg │ msg │ msg │
│  0  │  1  │  2  │  3  │  4  │
└─────┴─────┴─────┴─────┴─────┘
  ↑                         ↑
Offset 0                 Offset 4
```

#### (5) Consumer Group
- 같은 목적을 가진 Consumer들의 그룹
- 한 Partition은 그룹 내 한 Consumer만 읽을 수 있음
- 병렬 처리를 위한 핵심 개념

**Consumer Group 동작 방식**:
```
Consumer Group: coupon-issue-group

┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Consumer 1  │    │ Consumer 2  │    │ Consumer 3  │
│ Partition 0 │    │ Partition 1 │    │ Partition 2 │
└─────────────┘    └─────────────┘    └─────────────┘
```

**중요**: Consumer 수 > Partition 수이면 일부 Consumer는 유휴 상태

#### (6) Replication
- 데이터 손실 방지를 위한 복제본
- Replication Factor = 3이면 동일 데이터가 3개 Broker에 저장
- **Leader**: 읽기/쓰기를 담당
- **Follower**: Leader의 데이터를 복제만 수행

```
Partition 0의 Replication Factor = 3

Broker 1 (Leader): [msg1] [msg2] [msg3]
Broker 2 (Follower): [msg1] [msg2] [msg3]
Broker 3 (Follower): [msg1] [msg2] [msg3]
```

### 3.3 메시지 흐름

```
1. Producer가 메시지 생성
   ↓
2. Partitioner가 메시지를 어느 Partition에 보낼지 결정
   (Key가 있으면 Key의 Hash 값으로 결정)
   ↓
3. Leader Partition에 메시지 저장
   ↓
4. Follower Partition들이 복제
   ↓
5. Producer에게 ACK 응답
   ↓
6. Consumer가 메시지 읽기
   ↓
7. Consumer가 Offset Commit
```

---

## 4. Kafka 사용법 및 주의사항

### 4.1 기본 사용법

#### (1) Producer로 메시지 보내기

```java
@Service
@RequiredArgsConstructor
public class CouponIssueFacade {
    private final KafkaTemplate<String, CouponIssueRequestEvent> kafkaTemplate;

    public void issueRequest(Long couponId, Long userId) {
        // 1. 이벤트 생성
        CouponIssueRequestEvent event = CouponIssueRequestEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .couponId(couponId)
            .userId(userId)
            .occurredAt(LocalDateTime.now())
            .build();

        // 2. Kafka로 전송 (Key는 couponId로 설정하여 같은 쿠폰은 같은 파티션으로)
        String key = couponId.toString();
        kafkaTemplate.send("coupon-issue-requests", key, event);
    }
}
```

#### (2) Consumer로 메시지 받기

```java
@Component
@RequiredArgsConstructor
public class CouponIssueKafkaConsumer {

    @KafkaListener(
        topics = "coupon-issue-requests",
        groupId = "coupon-issue-group"
    )
    public void consume(
        @Payload CouponIssueRequestEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment ack
    ) {
        log.info("메시지 수신 - partition: {}, offset: {}, couponId: {}",
                 partition, offset, event.getCouponId());

        // 메시지 처리
        couponService.issueCoupon(event.getCouponId(), event.getUserId());

        // Offset Commit (수동)
        ack.acknowledge();
    }
}
```

### 4.2 설정 시 주의사항

#### (1) Broker 수 결정

**원칙**: Replication Factor보다 많아야 함

| 환경 | Broker 수 | Replication Factor | 설명 |
|-----|----------|-------------------|------|
| **로컬** | 1개 | 1 | 개발/테스트 환경 |
| **개발** | 3개 | 2 | 최소 고가용성 |
| **운영** | 5-7개 | 3 | 안정적인 운영 |

**왜 홀수인가?**
- Zookeeper의 과반수 투표 방식 때문
- 3개: 1대 장애 허용
- 5개: 2대 장애 허용

**Broker 추가/제거 시 주의사항**:

**(1) Broker 추가는 가능하지만...**
```
초기: 3개 Broker → 확장: 5개 Broker로 증가 ✅ 가능
```

**주의점**:
- 새 Broker를 추가해도 **기존 Partition은 자동으로 이동하지 않음**
- 새로 생성되는 Topic/Partition만 새 Broker에 배치됨
- 부하 분산을 위해서는 **수동으로 Partition 재할당(Reassignment) 필요**

```bash
# Partition 재할당 (운영 중 수행 시 부하 발생 주의!)
kafka-reassign-partitions --bootstrap-server localhost:9092 \
  --reassignment-json-file reassign.json --execute
```

**(2) Broker 제거는 신중하게**
- Replication Factor보다 적어지면 안 됨
- 해당 Broker의 Leader Partition을 먼저 다른 Broker로 이동 필요
- 데이터 유실 방지를 위해 Partition 재할당 후 제거

**결론**: Broker 수도 처음부터 적절하게 설정하는 것이 좋습니다.
- 추가는 가능하지만 Partition 재할당 작업이 번거로움
- 운영 중 재할당은 클러스터에 부하를 줄 수 있음

#### (2) Partition 수 결정

**공식**: `Partition 수 = 목표 처리량 / Consumer 1개당 처리량`

**예시**:
```
목표 처리량: 300 TPS
Consumer 1개당 처리량: 100 TPS
→ Partition 수 = 300 / 100 = 3개
```

**⚠️ 중요한 제약사항**:

**(1) Partition 수는 감소 불가능!**
```
❌ 불가능: 10개 → 5개로 줄이기
✅ 가능: 3개 → 6개로 늘리기
```

- **왜 줄일 수 없나?**
  - 각 Partition은 독립적인 파일 시스템 디렉토리
  - 데이터를 안전하게 병합할 방법이 없음
  - 줄이려면 Topic을 삭제하고 새로 만들어야 함 (데이터 손실!)

**(2) Partition 증가 시 주의사항**

파티션 수를 늘리면 **메시지 분배 규칙이 변경**됩니다!

```java
// 기존: 3개 파티션
hash(key) % 3 = 파티션 번호
  userId=100 → hash(100) % 3 = 1 → Partition 1

// 증가 후: 6개 파티션
hash(key) % 6 = 파티션 번호
  userId=100 → hash(100) % 6 = 4 → Partition 4 (변경됨!)
```

**영향**:
- 같은 Key의 메시지가 다른 Partition으로 분배됨
- **순서 보장이 깨질 수 있음**
- 진행 중인 데이터가 있다면 문제 발생 가능

**결론**: **처음부터 신중하게 결정하세요!**
- 초기 설정 시 향후 트래픽 증가를 고려
- 약간 여유있게 설정 (2-3배)
- 나중에 늘리는 것보다 처음부터 적절하게

**주의사항**:
- 너무 많으면: 메타데이터 오버헤드, 리소스 낭비, 리밸런싱 느림
- 너무 적으면: 병렬 처리 제한, 확장성 부족

**권장 가이드**:
- **소규모** (현재 트래픽 < 100 TPS): 3-6개
- **중규모** (100-1000 TPS): 10-30개
- **대규모** (1000+ TPS): 50-100개

#### (3) Replication Factor

**권장값**: 3

```
Replication Factor = 1: 데이터 손실 위험 (로컬만)
Replication Factor = 2: 1대 장애 허용 (개발)
Replication Factor = 3: 2대 장애 허용 (운영)
```

**성능 vs 안정성**:
- Replication이 높을수록 안정성 ↑, 성능 ↓
- 운영 환경에서는 3을 권장

#### (4) ISR (In-Sync Replicas)

ISR은 Leader와 동기화된 Replica 목록입니다.

**설정**: `min.insync.replicas = 2`
- Producer의 `acks=all` 설정 시 최소 2개 Replica에 저장되어야 ACK
- Replication Factor = 3, ISR = 2이면 1대 장애 허용

#### (5) Retention (보관 기간)

메시지를 얼마나 오래 보관할 것인가?

```yaml
# 시간 기반
log.retention.hours=168  # 7일

# 용량 기반
log.retention.bytes=1073741824  # 1GB
```

**트레이드오프**:
- 길게: 재처리 가능, 디스크 사용량 ↑
- 짧게: 디스크 절약, 재처리 불가

### 4.3 성능 튜닝 포인트

#### (1) Producer 설정

```yaml
# 배치 크기 (클수록 처리량 ↑, 지연 ↑)
batch.size: 16384  # 16KB

# 배치가 찰 때까지 대기 시간
linger.ms: 10  # 10ms

# 압축 (네트워크 트래픽 ↓)
compression.type: snappy  # lz4, gzip, zstd
```

#### (2) Consumer 설정

```yaml
# 한 번에 가져올 메시지 수
max.poll.records: 100

# Fetch 최소 크기
fetch.min.bytes: 1024  # 1KB

# Fetch 대기 시간
fetch.max.wait.ms: 500  # 500ms
```

---

## 5. Offset Commit: Auto vs Manual

### 5.1 Auto Commit (자동 커밋)

#### 동작 방식
```java
// 설정
enable.auto.commit=true
auto.commit.interval.ms=5000  // 5초마다 자동 커밋

// Consumer 코드
@KafkaListener(topics = "my-topic")
public void consume(String message) {
    // 메시지 처리
    processMessage(message);

    // 5초마다 자동으로 Offset Commit됨
}
```

#### 장점
- 구현이 간단
- 별도 Offset 관리 불필요

#### 단점 (심각한 문제!)

**(1) 메시지 중복 처리 가능**
```
1. Consumer가 메시지 읽음 (offset: 10)
2. 처리 중... (5초 안에 완료 못함)
3. Auto Commit 실행 → offset: 11로 커밋
4. Consumer 장애 발생
5. 재시작 → offset 11부터 읽음
6. offset 10 메시지는 처리 안 된 상태인데 건너뛰어짐!
```

**(2) 메시지 손실 가능**
```
1. Consumer가 메시지 100개 읽음 (offset: 0-99)
2. Auto Commit → offset: 100으로 커밋
3. 메시지 처리 시작
4. 50번째 메시지에서 에러 발생
5. Consumer 재시작 → offset 100부터 읽음
6. 50-99번 메시지 손실!
```

### 5.2 Manual Commit (수동 커밋) ⭐ 권장

#### 동작 방식
```java
// 설정
enable.auto.commit=false

// Consumer 코드
@KafkaListener(topics = "coupon-issue-requests")
public void consume(
    @Payload CouponIssueRequestEvent event,
    Acknowledgment ack  // Manual Commit을 위한 파라미터
) {
    try {
        // 1. 메시지 처리
        couponService.issueCoupon(event.getCouponId(), event.getUserId());

        // 2. 성공 시에만 Offset Commit
        ack.acknowledge();

    } catch (Exception e) {
        // 3. 실패 시 Commit 안 함 → 재처리됨
        log.error("처리 실패, 재시도 필요", e);
    }
}
```

#### 장점

**(1) 정확한 메시지 처리 보장**
- 처리 성공 시에만 Commit
- 실패 시 재처리 가능

**(2) At-Least-Once 처리 보장**
- 메시지가 최소 1번은 처리됨
- 중복은 가능하지만 손실은 없음

#### 단점
- 구현이 조금 복잡
- Offset 관리 필요

### 5.3 우리 프로젝트에서 Manual Commit을 선택한 이유

#### 쿠폰 발급 시나리오
```
사용자가 1만원 쿠폰 발급 요청
 ↓
Kafka로 이벤트 발행
 ↓
Consumer가 처리
```

**Auto Commit 사용 시 문제**:
```
1. 메시지 읽음: "사용자 A에게 1만원 쿠폰 발급"
2. Auto Commit → Offset 증가
3. DB에 쿠폰 저장 시도 → 에러 발생!
4. Consumer 재시작
5. 해당 메시지 건너뛰어짐
→ 사용자 A는 쿠폰을 받지 못함! (고객 불만)
```

**Manual Commit 사용 시**:
```
1. 메시지 읽음: "사용자 A에게 1만원 쿠폰 발급"
2. DB에 쿠폰 저장 시도
3. 성공 → ack.acknowledge() 호출
4. 실패 → Commit 안 함
5. Consumer 재시작 시 다시 읽어서 재시도
→ 반드시 처리됨! (고객 만족)
```

### 5.4 Manual Commit 구현 패턴

#### (1) 기본 패턴
```java
@KafkaListener(topics = "my-topic")
public void consume(String message, Acknowledgment ack) {
    try {
        process(message);
        ack.acknowledge();  // 성공 시 Commit
    } catch (Exception e) {
        // 실패 시 Commit 안 함
        log.error("처리 실패", e);
    }
}
```

#### (2) 재시도 + DLQ 패턴 (우리 프로젝트)
```java
@KafkaListener(topics = "coupon-issue-requests")
public void consume(CouponIssueRequestEvent event, Acknowledgment ack) {
    int maxRetries = 3;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            // 처리
            couponService.issueCoupon(event);

            // 성공 → Commit
            ack.acknowledge();
            return;

        } catch (Exception e) {
            log.warn("재시도 {}/{}", attempt, maxRetries);

            if (attempt < maxRetries) {
                // 지수 백오프 (1초, 2초, 3초)
                Thread.sleep(attempt * 1000);
            } else {
                // 최종 실패 → DLQ로 전송
                sendToDlq(event, e);
                ack.acknowledge();  // DLQ 전송 후 Commit
            }
        }
    }
}
```

---

## 6. 주니어 개발자를 위한 추가 팁

### 6.1 Kafka vs 다른 메시징 시스템

| 특징 | Kafka | RabbitMQ | Redis Pub/Sub |
|-----|-------|----------|--------------|
| **성능** | 매우 높음 (수백만 TPS) | 높음 (수만 TPS) | 매우 높음 |
| **지속성** | 디스크 저장 | 디스크 저장 | 메모리 (휘발성) |
| **보관 기간** | 설정 가능 (일/주) | 소비 후 삭제 | 없음 (즉시 삭제) |
| **재처리** | 가능 (Offset 조정) | 불가능 | 불가능 |
| **복잡도** | 높음 | 중간 | 낮음 |
| **용도** | 대용량 이벤트 스트리밍 | 작업 큐 | 간단한 알림 |

### 6.2 Consumer Lag 모니터링

Consumer Lag = Producer가 쓴 메시지 수 - Consumer가 읽은 메시지 수

```bash
# Lag 확인
docker exec kafka-container kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group coupon-issue-group
```

**출력 예시**:
```
GROUP           TOPIC               PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
coupon-issue-group  coupon-issue-requests  0          1000            1000        0
coupon-issue-group  coupon-issue-requests  1          1000            1050        50
```

**해석**:
- Partition 0: Lag = 0 (정상)
- Partition 1: Lag = 50 (지연 발생)

**대응**:
- Lag < 100: 정상
- Lag > 1000: Consumer 수 증가 필요
- Lag 계속 증가: 심각한 문제 (처리 속도 < 생산 속도)

### 6.3 메시지 순서 보장

**중요**: Kafka는 **Partition 내**에서만 순서 보장!

```
Topic: user-events (3 partitions)

Partition 0: [msg1] [msg3] [msg7]  ← 순서 보장
Partition 1: [msg2] [msg5] [msg8]  ← 순서 보장
Partition 2: [msg4] [msg6] [msg9]  ← 순서 보장

전체 순서: 보장 안 됨!
```

**순서가 중요한 경우**:
```java
// 같은 사용자의 이벤트는 같은 Partition으로
kafkaTemplate.send("user-events", userId.toString(), event);
//                                 ↑ Key를 userId로 설정
```

### 6.4 멱등성 (Idempotence) 고려

**문제**: Network 장애 시 메시지 중복 가능

```
Producer: 메시지 전송 → Network 타임아웃 → 재전송
                                           ↓
                                    중복 메시지 발생!
```

**해결책**:

#### (1) Producer 멱등성 설정
```yaml
enable.idempotence: true  # Kafka가 중복 제거
```

#### (2) Consumer에서 멱등성 처리
```java
@Transactional
public void issueCoupon(Long couponId, Long userId) {
    // 1. 중복 체크 (Redis SADD)
    boolean isNew = redisService.addIfNotExists(couponId, userId);

    if (!isNew) {
        log.warn("이미 처리된 요청");
        return;  // 중복 처리 방지
    }

    // 2. 쿠폰 발급
    UserCoupon userCoupon = new UserCoupon(userId, couponId);
    userCouponRepository.save(userCoupon);
}
```

### 6.5 트랜잭션 처리

Kafka는 정확히 한 번(Exactly-Once) 처리를 지원합니다.

```java
// Producer 설정
props.put("enable.idempotence", true);
props.put("transactional.id", "my-transactional-id");

// 트랜잭션 사용
kafkaTemplate.executeInTransaction(operations -> {
    operations.send("topic-1", message1);
    operations.send("topic-2", message2);
    return true;  // 둘 다 성공 또는 둘 다 실패
});
```

**주의**: 트랜잭션은 성능 오버헤드가 있으므로 필요한 경우만 사용

### 6.6 디버깅 팁

#### (1) 메시지 확인
```bash
# 토픽의 메시지 읽기
docker exec kafka-container kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic coupon-issue-requests \
  --from-beginning \
  --max-messages 10
```

#### (2) 토픽 정보 확인
```bash
# 토픽 상세 정보
docker exec kafka-container kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic coupon-issue-requests
```

#### (3) Consumer Group 상태
```bash
# Consumer Group 목록
docker exec kafka-container kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list

# Consumer Group 상세
docker exec kafka-container kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group coupon-issue-group
```

### 6.7 장애 대응

#### (1) Consumer 멈춤
```
증상: Lag이 계속 증가
원인: Consumer 에러, 무한 루프
대응: 로그 확인 → 에러 수정 → 재시작
```

#### (2) Broker 장애
```
증상: Producer/Consumer 연결 실패
원인: Broker 다운, 네트워크 문제
대응: Broker 재시작, Replication으로 자동 복구
```

#### (3) 메시지 처리 실패
```
증상: DLQ에 메시지 계속 쌓임
원인: 비즈니스 로직 에러
대응: DLQ 메시지 분석 → 버그 수정 → 수동 재처리
```

---

## 7. 실전 체크리스트

### 개발 시
- [ ] Topic 이름은 명확하고 일관성 있게 (kebab-case 권장)
- [ ] **Partition 수는 신중하게 결정** (나중에 줄일 수 없음!)
- [ ] 향후 트래픽 증가를 고려하여 여유있게 설정 (2-3배)
- [ ] Producer Key를 설정하여 순서 보장 필요 시 대비
- [ ] Consumer에서 Manual Commit 사용
- [ ] 재시도 로직 구현
- [ ] DLQ 설정

### 배포 시
- [ ] Replication Factor ≥ 2
- [ ] min.insync.replicas 설정
- [ ] Retention 정책 설정
- [ ] 모니터링 설정 (Consumer Lag)
- [ ] 알람 설정 (Lag 임계치)

### 운영 시
- [ ] 주기적으로 Consumer Lag 확인
- [ ] DLQ 메시지 모니터링
- [ ] 디스크 사용량 확인
- [ ] Broker 상태 확인

---

## 8. 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Confluent Kafka Tutorials](https://kafka-tutorials.confluent.io/)
- [STEP18_REPORT.md](./STEP18_REPORT.md) - Kafka 기반 쿠폰 발급 시스템 설계
- [STEP18_IMPLEMENTATION_NOTES.md](./STEP18_IMPLEMENTATION_NOTES.md) - 실제 구현 가이드
- [KAFKA_COUPON_SETUP.md](./KAFKA_COUPON_SETUP.md) - Kafka 설정 가이드

---

## 9. 마무리

### 핵심 정리

1. **Kafka는 대용량 실시간 이벤트 스트리밍 플랫폼**
   - 높은 처리량, 내구성, 확장성

2. **Partition과 Consumer Group으로 병렬 처리**
   - Partition 수 = 최대 병렬도

3. **⚠️ Partition 수는 감소 불가능! 처음부터 신중하게 결정**
   - 증가는 가능하지만 메시지 분배 규칙이 변경됨 (순서 보장 영향)
   - Broker 추가도 가능하지만 Partition 재할당 필요
   - 향후 트래픽을 고려하여 여유있게 설정

4. **Manual Commit으로 정확한 메시지 처리 보장**
   - Auto Commit은 메시지 손실/중복 가능
   - Manual Commit은 At-Least-Once 보장

5. **적절한 설정이 중요**
   - Broker 수, Partition 수, Replication Factor
   - 환경에 따라 다르게 설정

6. **모니터링 필수**
   - Consumer Lag, DLQ, Broker 상태

### 다음 단계

- [ ] 로컬 환경에서 Kafka 실습
- [ ] 간단한 Producer/Consumer 구현
- [ ] Consumer Lag 모니터링 경험
- [ ] 장애 상황 시뮬레이션 (Broker 중지, Consumer 에러)
- [ ] 성능 테스트 (처리량, 지연시간)

**Remember**: Kafka는 강력하지만 복잡합니다. 작은 것부터 시작하여 점진적으로 확장하세요!
