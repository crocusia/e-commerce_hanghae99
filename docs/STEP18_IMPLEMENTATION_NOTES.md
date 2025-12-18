# STEP18 실제 구현 요약

## 구현 방식

### 간단한 버전 (실제 구현)

현재 구현은 **배치 처리 없이 메시지를 하나씩 순차 처리**하는 방식입니다.

#### 특징
- **메시지 처리**: Kafka Consumer가 메시지를 하나씩 받아 즉시 처리
- **재시도**: 3회까지 재시도 (1초, 2초, 3초 지수 백오프)
- **DLQ**: 최종 실패 시 Dead Letter Queue로 전송
- **간단함**: 구현이 단순하고 이해하기 쉬움
- **충분한 성능**: 100-200 TPS (기존 20 TPS 대비 5-10배 향상)

#### 토픽 설정 (로컬 환경)
```yaml
coupon-issue-requests:
  Partitions: 3
  Replication Factor: 1

coupon-issue-dlq:
  Partitions: 1
  Replication Factor: 1
```

#### Consumer 설정
```yaml
Consumer Group: coupon-issue-group
Concurrency: 3 (파티션당 1개)
ACK Mode: MANUAL
메시지 처리: 하나씩 순차 처리
```

### 성능 비교

| 항목 | 기존 (Scheduler) | 개선 (Kafka 간단한 버전) | Kafka 배치 버전 (선택) |
|-----|----------------|----------------------|---------------------|
| **처리 방식** | 5초 폴링 | 실시간 이벤트 | 실시간 이벤트 |
| **처리 지연** | 평균 2.5초 | 평균 50ms | 평균 50ms |
| **처리량** | 20 TPS | 100-200 TPS | 300+ TPS |
| **구현 복잡도** | 낮음 | **낮음** | 중간 |
| **확장성** | 제한적 | **좋음** | 매우 좋음 |

### 왜 간단한 버전을 선택했나?

#### 1. 충분한 성능
- 기존 20 TPS → 100-200 TPS (5-10배 향상)
- 대부분의 쿠폰 발급 시나리오에서 충분

#### 2. 낮은 복잡도
- 배치 처리 로직 불필요
- 트랜잭션 관리 단순
- 디버깅 쉬움

#### 3. 유지보수 용이
- 코드가 짧고 명확함
- 새로운 개발자도 쉽게 이해
- 버그 발생 가능성 낮음

#### 4. 점진적 개선 가능
- 필요 시 배치 처리 추가 가능
- 파티션 수 증가로 확장 가능

## 배치 처리 추가 방법 (선택적)

필요 시 아래와 같이 배치 처리를 추가할 수 있습니다.

### 1. Kafka Config 수정

```java
// Consumer 설정에 배치 파라미터 추가
configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // 한 번에 100개
configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // 최소 1KB
configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // 최대 500ms 대기
```

### 2. Consumer 로직 수정

```java
@KafkaListener(topics = "coupon-issue-requests", ...)
public void consumeCouponIssueRequests(
    @Payload List<CouponIssueRequestEvent> events, // List로 변경
    @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
    @Header(KafkaHeaders.OFFSET) List<Long> offsets,
    Acknowledgment ack
) {
    log.info("[Kafka Consumer] 배치 처리 시작 - size: {}", events.size());

    // 배치로 DB 처리
    List<UserCoupon> userCoupons = events.stream()
        .map(event -> userCouponService.issueCouponAsync(
            event.getCouponId(), event.getUserId()))
        .toList();

    // 성공 시 Offset Commit
    ack.acknowledge();
}
```

### 3. 성능 향상

```
배치 크기: 100개
Consumer: 3개
  ↓
처리량: 100 * 3 = 300 TPS
  ↓
10,000건 처리: ~31초 (기존 8분 20초)
```

## 실제 배포 시 체크리스트

### 1. Docker Compose 실행
```bash
docker-compose up -d
```

### 2. Kafka 준비 대기 (10-20초)
```bash
docker-compose logs -f kafka
# "started (kafka.server.KafkaServer)" 메시지 확인
```

### 3. 토픽 생성
```bash
# Windows
cd scripts
create-kafka-topics.bat

# Linux/Mac
cd scripts
chmod +x create-kafka-topics.sh
./create-kafka-topics.sh
```

### 4. 토픽 확인
```bash
docker exec kafka-container kafka-topics --list --bootstrap-server localhost:9092
```

예상 출력:
```
coupon-issue-dlq
coupon-issue-requests
order-created-events
```

### 5. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 6. Kafka UI 확인
- URL: http://localhost:8989
- 토픽, 메시지, Consumer Group 상태 확인

## 모니터링 포인트

### 1. Consumer Lag
```bash
docker exec kafka-container kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group coupon-issue-group
```

- **LAG = 0**: 정상 (실시간 처리)
- **LAG > 100**: 주의 (처리 지연)
- **LAG 계속 증가**: 경고 (Consumer 수 증가 필요)

### 2. DLQ 메시지 수
```bash
docker exec kafka-container kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic coupon-issue-dlq --from-beginning --max-messages 10
```

- 메시지가 계속 쌓이면 문제 원인 파악 필요

### 3. 애플리케이션 로그
```bash
tail -f logs/application.log | grep "Kafka Consumer"
```

- 실패 메시지 확인
- 재시도 횟수 모니터링

## 성능 튜닝 가이드

### 현재 처리량이 부족한 경우

#### 1. Consumer 수 증가 (가장 쉬움)
```java
// KafkaConfig.java
factory.setConcurrency(6); // 3 → 6
```

#### 2. 파티션 수 증가
```bash
docker exec kafka-container kafka-topics --alter \
  --bootstrap-server localhost:9092 \
  --topic coupon-issue-requests \
  --partitions 6
```

**⚠️ 주의사항**:
- 파티션 수는 늘릴 수만 있고 줄일 수 없음
- 파티션 증가 시 메시지 분배 규칙 변경 (hash % partition 수)
- 순서 보장이 필요한 경우 진행 중인 메시지가 모두 처리된 후 증가 권장

#### 3. 배치 처리 추가 (복잡함)
- 위의 "배치 처리 추가 방법" 참고

### 권장 순서
1. Consumer 수 증가 (쉬움, 즉시 효과)
2. 파티션 수 증가 (중간, 확장성 향상)
3. 배치 처리 추가 (복잡함, 최대 성능)

## 운영 환경 권장 설정

### 토픽 설정
```yaml
coupon-issue-requests:
  Partitions: 6-12 (트래픽에 따라)
  Replication Factor: 3 (고가용성)
  Retention: 7 days
  Min In-Sync Replicas: 2
```

### Consumer 설정
```yaml
Consumer Group: coupon-issue-group
Concurrency: 6-12 (파티션 수와 동일 또는 절반)
ACK Mode: MANUAL
Auto Offset Reset: earliest
```

### Producer 설정
```yaml
ACKS: all
Retries: 3
Idempotence: true
```

## 참고 문서

- [STEP18_REPORT.md](./STEP18_REPORT.md) - 전체 설계 문서
- [KAFKA_COUPON_SETUP.md](./KAFKA_COUPON_SETUP.md) - Kafka 설정 가이드
- [scripts/README.md](../scripts/README.md) - 스크립트 사용법
