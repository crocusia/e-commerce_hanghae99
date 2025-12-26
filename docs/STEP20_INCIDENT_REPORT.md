# STEP 20: 선착순 쿠폰 발급 시스템 장애 대응 보고서

## 문서 정보
- **작성일**: 2025-12-26
- **작성자**: Backend Team
- **장애 심각도**: High
- **서비스**: 선착순 쿠폰 발급 API

---

## 요약 (Executive Summary)

2025년 12월 26일 14:00, 선착순 쿠폰 이벤트 진행 중 Redis Connection Pool 고갈로 인한 쿠폰 발급 API 장애가 발생했습니다. 트래픽 급증 시 응답 지연 및 높은 에러율(8.5%)이 관측되었으며, Connection Pool 증설 등의 긴급 조치를 통해 약 10분 만에 서비스를 복구했습니다.

---

# 1. 현상

## 1.1 타임라인

| 시간 | 이벤트 | 담당자/시스템 | 상세 내용 |
|------|--------|---------------|-----------|
| **14:00:00** | 🎯 이벤트 시작 | 마케팅팀 | 선착순 쿠폰 이벤트 공지 발송 |
| 14:00:10 | 📈 트래픽 유입 시작 | 모니터링 | RPS 100 → 300으로 증가 |
| **14:00:30** | ⚠️ 트래픽 급증 감지 | 모니터링 시스템 | RPS 1000+ 돌파 (예상치 5배 초과) |
| **14:01:00** | 🐌 응답 지연 시작 | API 서버 | p95: 500ms → 2000ms로 급증 |
| **14:01:30** | 🔴 Redis 에러 발생 | Redis Client | `Connection timeout` 에러 발생 시작 |
| 14:02:00 | 📊 에러율 30% 도달 | API 서버 | 10명 중 3명이 에러 응답 수신 |
| **14:02:30** | 🚨 장애 인지 | 온콜 엔지니어 | PagerDuty 알람 수신 (Critical) |
| 14:03:00 | 🔍 원인 분석 시작 | 엔지니어 | Grafana/로그 확인 시작 |
| 14:04:00 | 💡 원인 파악 | 엔지니어 | Redis Connection Pool 고갈 확인 |
| **14:05:00** | ⚡ 긴급 조치 결정 | 엔지니어 | Connection Pool 10 → 50 증설 결정 |
| 14:06:00 | 🔧 설정 변경 | 엔지니어 | application.yml 수정 |
| **14:07:00** | 🚀 재배포 완료 | CI/CD | 애플리케이션 재시작 (약 1분 소요) |
| 14:08:00 | 📉 에러율 감소 시작 | API 서버 | 30% → 10% → 5% |
| **14:10:00** | ✅ 완전 복구 | API 서버 | 에러율 < 1%, 응답시간 정상화 |
| 14:15:00 | 👀 사후 모니터링 | 엔지니어 | 30분간 집중 모니터링 (재발 없음) |

**총 장애 시간**: 약 **10분** (14:00:30 ~ 14:10:00)

## 1.2 영향 범위

### 기술적 영향
| 구분 | 영향 내용 |
|------|-----------|
| **영향 받은 API** | `POST /api/user-coupons/issue-async` |
| **영향 받은 컴포넌트** | Redis Connection Pool, Kafka Producer |
| **정상 동작 컴포넌트** | MySQL, Kafka Consumer, 기타 API |

### 서비스 영향
- **쿠폰 발급 API**: 완전 장애 (에러율 30%)
- **쿠폰 조회 API**: 정상 동작
- **주문/결제 API**: 정상 동작
- **기타 서비스**: 영향 없음

## 1.3 고객 영향도 (비즈니스 임팩트)

### 정량적 지표
| 지표 | 수치 |
|------|------|
| **영향 받은 사용자 수** | 약 5,000명 (추정) |
| **실패한 요청 수** | 850건 |
| **성공한 요청 수** | 9,150건 |
| **총 요청 수** | 10,000건 |
| **실패율** | 8.5% |

### 비즈니스 영향
| 구분 | 내용 |
|------|------|
| **직접 손실** | - 850명의 쿠폰 발급 실패<br>- 잠재적 매출 손실: 약 425만원 (쿠폰당 5,000원 × 850건) |
| **간접 손실** | - 고객 불만 접수 약 50건<br>- SNS 부정적 언급 약 20건<br>- 브랜드 이미지 손상 |
| **기회 비용** | - 10분간 신규 발급 중단<br>- 이벤트 효과 반감 |

### 고객 불만 사례
```
고객 A: "계속 에러 나요. 쿠폰 다 떨어진 건가요?"
고객 B: "앱이 먹통이에요. 다시 해주세요."
고객 C: "이벤트 시작하자마자 서버 터지네요 ㅠㅠ"
```

---

# 2. 조치 내용

## 2.1 장애 원인

### 직접 원인 (Immediate Cause)
**Redis Connection Pool 고갈**

```yaml
# 설정값 (Before)
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 10      # ← 문제의 원인
          max-idle: 10
          min-idle: 2

# 실제 부하
- 피크 RPS: 1000 RPS
- 필요 Connection: 약 50개 (추정)
- 가용 Connection: 10개

결과:
  → Connection 대기 시간 증가
  → Timeout 에러 발생 (8.5%)
  → 응답 시간 급증 (2500ms)
```

### 근본 원인 (Root Cause)
1. **부하 테스트 부족**
   - 최대 테스트: 200 RPS
   - 실제 피크: 1000 RPS (5배 차이)
   - 병목 지점을 사전에 발견하지 못함

2. **기본값 맹신**
   - Connection Pool 기본값(10) 그대로 사용
   - 프로덕션 트래픽에 대한 용량 계획 부재

3. **모니터링 부재**
   - Redis Connection Pool 메트릭 미수집
   - Connection 사용률 알람 미설정
   - 장애 발생 후에야 인지

4. **팀 간 협업 부족**
   - 마케팅팀의 이벤트 규모 정보 미공유
   - 예상 트래픽 10배 오차 (100 RPS 예상 → 1000 RPS 실제)

## 2.2 해소 타임라인

### Phase 1: 장애 인지 (2분 30초)
```
14:00:30  ⚠️ 트래픽 급증
14:01:00  🐌 응답 지연 감지
14:01:30  🔴 에러 발생 시작
14:02:00  📊 에러율 30% 도달
14:02:30  🚨 알람 → 장애 인지
```

### Phase 2: 원인 분석 (1분 30초)
```
14:03:00  🔍 Grafana 대시보드 확인
          - CPU: 35% (정상)
          - Memory: 60% (정상)
          - Redis Connection: 10/10 (100% ← 문제!)

14:04:00  💡 원인 파악 완료
          - 로그: "Unable to acquire connection from pool"
          - 결론: Connection Pool 고갈
```

### Phase 3: 긴급 조치 (3분)
```
14:05:00  ⚡ 조치 결정
          - Connection Pool 증설: 10 → 50
          - DB Pool도 증설: 10 → 30

14:06:00  🔧 설정 변경
          - application.yml 수정
          - Git commit & push

14:07:00  🚀 재배포
          - 애플리케이션 재시작 (1분 소요)
```

### Phase 4: 복구 확인 (3분)
```
14:08:00  📉 개선 감지
          - 에러율: 30% → 10%
          - 응답 시간: 2500ms → 800ms

14:09:00  📉 지속 개선
          - 에러율: 10% → 3%
          - 응답 시간: 800ms → 400ms

14:10:00  ✅ 완전 복구
          - 에러율: < 1%
          - 응답 시간: p95 < 500ms
```

## 2.3 실제 단기 대응책 (Emergency Response)

### 긴급 조치 1: Redis Connection Pool 증설
```yaml
# Before
spring.data.redis.lettuce.pool.max-active: 10

# After
spring.data.redis.lettuce.pool.max-active: 50
spring.data.redis.lettuce.pool.max-idle: 50
spring.data.redis.lettuce.pool.min-idle: 10
```

**효과**:
- ✅ 500 RPS까지 안정적 처리 가능
- ✅ Connection timeout 에러 제거
- ✅ 응답 시간 82% 개선 (2500ms → 450ms)

### 긴급 조치 2: DB Connection Pool 증설
```yaml
# Before
spring.datasource.hikari.maximum-pool-size: 10

# After
spring.datasource.hikari.maximum-pool-size: 30
spring.datasource.hikari.minimum-idle: 10
spring.datasource.hikari.connection-timeout: 3000
```

**효과**:
- ✅ Consumer의 DB 처리 속도 향상
- ✅ Kafka 메시지 처리 지연 감소

### 긴급 조치 3: 모니터링 강화
```yaml
# Actuator 메트릭 활성화
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

**추가된 메트릭**:
- `redis.connection.active` - Redis 활성 연결 수
- `redis.connection.idle` - Redis 유휴 연결 수
- `hikari.connections.active` - DB 활성 연결 수

## 2.4 후속 대응 계획

### 즉시 실행 (당일)
- [x] Connection Pool 증설
- [x] 모니터링 메트릭 추가
- [x] 알람 설정 (Connection Pool 80% 사용 시)
- [ ] 고객 공지 및 사과문 발송
- [ ] 장애 보고서 작성

### 1주 내
- [ ] Circuit Breaker 패턴 도입
- [ ] Rate Limiting 적용
- [ ] 부하 테스트 재수행 (2배 트래픽)
- [ ] Runbook 작성

### 1개월 내
- [ ] Redis Cluster 구성 (고가용성)
- [ ] Kafka 파티션 증가 (처리량 향상)
- [ ] Auto Scaling 설정 (HPA)
- [ ] 이벤트 사전 리뷰 프로세스 수립

---

# 3. 분석

## 3.1 5 Whys 분석

### Q1: 왜 장애가 발생했는가?
**A1**: Redis Connection Pool이 고갈되어 Connection timeout 에러가 발생했기 때문

### Q2: 왜 Connection Pool이 고갈되었는가?
**A2**: Connection Pool 크기가 10개로 제한되어 있었고, 1000 RPS의 트래픽을 감당할 수 없었기 때문

### Q3: 왜 10개로 제한되어 있었는가?
**A3**: 기본값을 그대로 사용했고, 부하 테스트로 이 문제를 발견하지 못했기 때문

### Q4: 왜 부하 테스트로 발견하지 못했는가?
**A4**: 부하 테스트의 최대 트래픽이 200 RPS로, 실제 피크 트래픽(1000 RPS)의 1/5 수준에 불과했기 때문

### Q5: 왜 실제 트래픽을 예측하지 못했는가?
**A5**: 마케팅팀과 엔지니어팀 간의 이벤트 규모에 대한 정보 공유가 부족했고, 이벤트 사전 리뷰 프로세스가 없었기 때문

### 진짜 근본 원인 (True Root Cause)
> **팀 간 커뮤니케이션 부재 + 사전 검증 프로세스 부재**

## 3.2 부하 테스트 결과 분석

### Baseline Test (정상 부하)
```
설정:
  VU: 100명
  Duration: 1분
  RPS: 약 100

결과:
  ✅ p50: 120ms
  ✅ p95: 350ms (목표: <500ms)
  ✅ p99: 480ms (목표: <1000ms)
  ✅ Error Rate: 0.2% (목표: <1%)

시스템 상태:
  - CPU: 35%
  - Memory: 60%
  - Redis Connections: 8/10 (80% ← 여유 부족)

분석:
  정상 트래픽에서는 문제 없으나,
  Connection Pool 사용률 80%로 여유가 부족함
```

### Spike Test (피크 부하) - **장애 발생**
```
설정:
  VU: 10 → 500 → 10
  Duration: 2분
  Peak RPS: 500

결과:
  ❌ p50: 450ms
  ❌ p95: 2500ms (목표: <1000ms) - 2.5배 초과
  ❌ p99: 5800ms (목표: <1000ms) - 5.8배 초과
  ❌ Error Rate: 8.5% (목표: <5%) - 1.7배 초과

시스템 상태:
  - CPU: 85% (여유 있음)
  - Memory: 75% (여유 있음)
  - Redis Connections: 10/10 (100% ← 고갈!)

에러 종류:
  - Connection timeout: 85%
  - Read timeout: 10%
  - Unknown: 5%

분석:
  ⚠️ Redis Connection Pool이 명확한 병목
  CPU/Memory는 여유 있으나 I/O 대기로 인한 성능 저하
  500 RPS 이상에서 급격한 성능 저하 발생
```

## 3.3 병목 지점 상세 분석

### 병목 1순위: Redis Connection Pool ⭐⭐⭐
```
문제:
  - 설정: max-active: 10
  - 필요: 약 50개 (500 RPS 기준)
  - 결과: Connection 대기 → Timeout

근거:
  - 로그: "Unable to acquire connection from pool"
  - 메트릭: redis.connection.active = 10/10 (지속)
  - 증상: Connection timeout 에러 85%

영향도: Critical
```

### 병목 2순위: Kafka Producer 동기 전송 ⭐⭐
```
문제:
  kafkaTemplate.send(topic, event).get(); // 전송 완료 대기

  - Blocking I/O로 인한 처리량 제한
  - 메시지당 50-100ms 대기

개선 방향:
  비동기 전송으로 변경
  kafkaTemplate.send(topic, event); // 즉시 리턴

예상 효과:
  응답 시간 50-100ms 단축
```

### 병목 3순위: DB Connection Pool ⭐
```
현재 상태:
  - maximum-pool-size: 10
  - Consumer에서 DB 사용
  - 피크 시 부족 가능성 있음

조치:
  10 → 30 증설 완료

효과:
  Consumer 처리 속도 향상
```

---

# 4. 대응 방안 액션 아이템

## 4.1 Short-term (1주 이내)

### [P0] Circuit Breaker 도입
**목적**: 연쇄 장애 방지 및 빠른 실패 처리

```java
@CircuitBreaker(name = "couponIssue", fallbackMethod = "issueFallback")
public void issueRequest(Long couponId, Long userId) {
    // 기존 로직
}

private void issueFallback(Long couponId, Long userId, Exception e) {
    log.error("Circuit breaker activated", e);
    throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE,
        "일시적인 오류입니다. 잠시 후 다시 시도해주세요.");
}
```

**설정**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      couponIssue:
        failure-rate-threshold: 50        # 실패율 50% 이상
        wait-duration-in-open-state: 10s  # 10초 대기
        sliding-window-size: 10           # 최근 10개 요청 기준
```

**담당**: Backend Team
**기한**: 2025-12-28
**우선순위**: P0 (Critical)

---

### [P0] Rate Limiting 적용
**목적**: 과도한 트래픽으로부터 시스템 보호

```java
@RateLimiter(name = "couponIssue")
@PostMapping("/issue-async")
public ResponseEntity<?> issueAsync(@RequestBody IssueCouponRequest request) {
    // 기존 로직
}
```

**설정**:
```yaml
resilience4j:
  ratelimiter:
    instances:
      couponIssue:
        limit-for-period: 500      # 기간당 요청 수
        limit-refresh-period: 1s   # 1초마다 갱신
        timeout-duration: 0s       # 즉시 거부
```

**담당**: Backend Team
**기한**: 2025-12-28
**우선순위**: P0 (Critical)

---

### [P1] Kafka Producer 비동기 전송
**목적**: Blocking I/O 제거로 처리량 향상

```java
// Before (Blocking)
kafkaTemplate.send(TOPIC, key, event).get(); // 전송 완료까지 대기

// After (Non-blocking)
kafkaTemplate.send(TOPIC, key, event)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Kafka 전송 실패", ex);
            // 보상 트랜잭션 or DLQ 전송
            handleKafkaFailure(event, ex);
        } else {
            log.debug("Kafka 전송 성공: offset={}",
                result.getRecordMetadata().offset());
        }
    });
```

**예상 효과**:
- 응답 시간 50-100ms 단축
- 처리량 2배 향상

**담당**: Backend Team
**기한**: 2025-12-30
**우선순위**: P1 (High)

---

### [P1] 부하 테스트 재수행
**목적**: 개선 효과 검증 및 추가 병목 발견

**테스트 계획**:
```
시나리오 1: Baseline (100 VU)
  - 목표: p95 < 300ms, Error < 0.5%

시나리오 2: Spike (500 VU)
  - 목표: p95 < 500ms, Error < 1%

시나리오 3: Extreme (1000 VU)
  - 목표: p95 < 1000ms, Error < 5%
```

**담당**: Backend Team
**기한**: 2025-12-31
**우선순위**: P1 (High)

---

### [P2] Runbook 작성
**목적**: 장애 시 빠른 대응을 위한 매뉴얼

**포함 내용**:
1. 장애 증상별 대응 절차
2. 주요 명령어 모음
3. Rollback 절차
4. 에스컬레이션 기준

**예시**:
```markdown
## Redis Connection Pool 고갈 시

증상:
  - "Unable to acquire connection" 에러
  - 응답 시간 급증

확인:
  curl http://localhost:8080/actuator/metrics/redis.connection.active

조치:
  1. application.yml 수정
  2. 애플리케이션 재시작
  3. 메트릭 확인
```

**담당**: Backend Team
**기한**: 2026-01-03
**우선순위**: P2 (Medium)

---

## 4.2 Mid-term (1개월 이내)

### [P0] Redis Cluster 구성
**목적**: 단일 장애점(SPOF) 제거 및 고가용성 확보

**아키텍처**:
```
Before: Redis Single Instance
  - 장애 시 전체 서비스 중단
  - 처리량 제한

After: Redis Cluster (3 Master + 3 Replica)
  - Master 장애 시 자동 Failover
  - 처리량 3배 증가
  - 데이터 샤딩으로 부하 분산
```

**설정**:
```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-node-1:6379
          - redis-node-2:6379
          - redis-node-3:6379
        max-redirects: 3
      lettuce:
        pool:
          max-active: 100  # Cluster 환경에서 증설
```

**예상 효과**:
- ✅ 고가용성: 99.9% → 99.99%
- ✅ 처리량: 500 RPS → 1500 RPS
- ✅ 장애 복구: 수동 → 자동 (30초 이내)

**담당**: DevOps Team + Backend Team
**기한**: 2026-01-20
**우선순위**: P0 (Critical)

---

### [P1] Kafka 파티션 증가
**목적**: Consumer 병렬 처리로 처리량 향상

```bash
# 현재: 1개 파티션
# 변경: 3개 파티션

kafka-topics.sh --alter \
  --topic coupon-issue-requests \
  --partitions 3 \
  --bootstrap-server localhost:9092
```

**효과**:
```
Before:
  Consumer 1개 → 파티션 1개
  처리량: 약 100 msg/s

After:
  Consumer 3개 → 파티션 3개
  처리량: 약 300 msg/s (3배)
```

**담당**: Backend Team
**기한**: 2026-01-15
**우선순위**: P1 (High)

---

### [P1] Prometheus + Grafana 구축
**목적**: 통합 모니터링 대시보드 구축

**구성**:
```
Spring Boot (Actuator)
      ↓
  Prometheus (메트릭 수집)
      ↓
   Grafana (시각화)
```

**대시보드 패널**:
1. API 응답 시간 (p50, p95, p99)
2. 에러율 (전체, API별)
3. Redis Connection Pool 사용률
4. DB Connection Pool 사용률
5. Kafka Producer/Consumer Lag
6. JVM 메모리/GC

**알람 설정**:
- Redis Connection > 80%: Warning
- Redis Connection > 90%: Critical
- Error Rate > 5%: Critical
- Response Time p95 > 1000ms: Warning

**담당**: DevOps Team
**기한**: 2026-01-25
**우선순위**: P1 (High)

---

### [P2] 이벤트 사전 리뷰 프로세스
**목적**: 마케팅 이벤트 전 기술 검토 필수화

**프로세스**:
```
1주일 전:
  마케팅팀 → 이벤트 계획서 공유
  - 예상 참여자 수
  - 피크 시간대
  - 지속 시간

3일 전:
  엔지니어팀 → 기술 검토
  - 부하 테스트 시나리오 수립
  - 용량 계획 (Scale up/out)
  - 모니터링 설정

1일 전:
  부하 테스트 수행
  문제 발견 시 → 이벤트 연기 or 규모 축소

이벤트 당일:
  엔지니어 On-call 배치
  실시간 모니터링
```

**담당**: CTO
**기한**: 2026-01-10
**우선순위**: P2 (Medium)

---

## 4.3 Long-term (3개월 이내)

### [P1] Auto Scaling (HPA) 구성
**목적**: 트래픽에 따른 자동 확장/축소

**설정** (Kubernetes HPA):
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ecommerce-api
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ecommerce-api
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: 200
```

**효과**:
- CPU 70% 초과 시 자동 Pod 증설
- RPS 200 초과 시 자동 Pod 증설
- 트래픽 감소 시 자동 축소 (비용 절감)

**담당**: DevOps Team
**기한**: 2026-03-15
**우선순위**: P1 (High)

---

### [P2] 대기열 시스템 도입
**목적**: 순간 트래픽 폭증 시 안정적 처리

**아키텍처**:
```
사용자 요청
    ↓
[대기열 시스템] Redis Sorted Set
    ↓
순차적 처리 (Rate Limiting)
    ↓
쿠폰 발급
```

**구현**:
```java
// 대기열 진입
public QueuePosition enterQueue(Long couponId, Long userId) {
    long timestamp = System.currentTimeMillis();
    long rank = redisTemplate.opsForZSet()
        .add("queue:coupon:" + couponId, userId.toString(), timestamp);

    return new QueuePosition(rank, estimatedWaitTime(rank));
}

// 대기 시간 안내
private long estimatedWaitTime(long rank) {
    long processingRate = 100; // 초당 100명 처리
    return rank / processingRate; // 초 단위
}
```

**사용자 경험**:
```
"현재 대기 순번: 523번
예상 대기 시간: 약 5초

잠시만 기다려주세요..."
```

**효과**:
- 트래픽 폭증 시에도 안정적 처리
- 사용자에게 명확한 대기 시간 안내
- 서버 부하 분산

**담당**: Backend Team
**기한**: 2026-03-31
**우선순위**: P2 (Medium)

---

### [P2] DB Read Replica 구성
**목적**: 읽기 부하 분산

**아키텍처**:
```
Before:
  MySQL Single Instance
  - 쓰기/읽기 모두 Master

After:
  MySQL Master-Replica
  - 쓰기 → Master
  - 읽기 → Replica (3대)
```

**효과**:
- 조회 쿼리 부하 75% 감소
- Master의 쓰기 성능 향상
- 장애 시 Read Replica를 Master로 승격 가능

**담당**: DevOps Team
**기한**: 2026-03-31
**우선순위**: P2 (Medium)

---

## 4.4 액션 아이템 요약표

| 우선순위 | 항목 | 담당 | 기한 | 상태 |
|---------|------|------|------|------|
| **Short-term (1주)** |
| P0 | Circuit Breaker 도입 | Backend | 12/28 | ⏳ In Progress |
| P0 | Rate Limiting 적용 | Backend | 12/28 | ⏳ In Progress |
| P1 | Kafka 비동기 전송 | Backend | 12/30 | 📋 Planned |
| P1 | 부하 테스트 재수행 | Backend | 12/31 | 📋 Planned |
| P2 | Runbook 작성 | Backend | 01/03 | 📋 Planned |
| **Mid-term (1개월)** |
| P0 | Redis Cluster 구성 | DevOps+Backend | 01/20 | 📋 Planned |
| P1 | Kafka 파티션 증가 | Backend | 01/15 | 📋 Planned |
| P1 | Prometheus+Grafana | DevOps | 01/25 | 📋 Planned |
| P2 | 이벤트 사전 리뷰 | CTO | 01/10 | 📋 Planned |
| **Long-term (3개월)** |
| P1 | Auto Scaling (HPA) | DevOps | 03/15 | 📋 Planned |
| P2 | 대기열 시스템 | Backend | 03/31 | 📋 Planned |
| P2 | DB Read Replica | DevOps | 03/31 | 📋 Planned |

---

# 5. 개선 효과 측정

## 5.1 Before vs After

### 성능 지표 비교
| 메트릭 | Before (장애) | After (개선) | 개선율 |
|--------|---------------|-------------|--------|
| **Baseline p95** | 350ms | 200ms | 43% ↓ |
| **Spike p95** | 2500ms | 450ms | 82% ↓ |
| **Spike Error Rate** | 8.5% | 0.3% | 96% ↓ |
| **최대 처리 RPS** | 200 | 600 | 200% ↑ |
| **Redis Connection 여유** | 0% | 60% | - |
| **DB Connection 여유** | 30% | 70% | - |

### 재테스트 결과 (개선 후)

#### Baseline Test
```
VU: 100
Duration: 1분
RPS: 약 100

결과:
  ✅ p50: 95ms (개선: 21%)
  ✅ p95: 200ms (개선: 43%)
  ✅ p99: 280ms (개선: 42%)
  ✅ Error Rate: 0.1%
  ✅ Redis Connections: 5/50 (10%)

결론: 목표 달성 ✓
```

#### Spike Test
```
VU: 10 → 500 → 10
Duration: 2분
Peak RPS: 500

결과:
  ✅ p50: 180ms (개선: 60%)
  ✅ p95: 450ms (개선: 82%)
  ✅ p99: 780ms (개선: 87%)
  ✅ Error Rate: 0.3% (개선: 96%)
  ✅ Redis Connections: 20/50 (40%)

결론: 목표 달성 ✓
```

## 5.2 비즈니스 지표 개선

| 지표 | Before | After | 개선 |
|------|--------|-------|------|
| **쿠폰 발급 성공률** | 91.5% | 99.7% | +8.2%p |
| **고객 만족도** | 3.2/5.0 | 4.5/5.0 | +1.3점 |
| **평균 응답 시간** | 1200ms | 250ms | 79% ↓ |
| **시간당 처리 가능 건수** | 720건 | 2,160건 | 200% ↑ |

---

# 6. 교훈 (Lessons Learned)

## 6.1 기술적 교훈

### 1. Connection Pool은 충분히 설정해야 한다
- ❌ 기본값 맹신 금지
- ✅ 예상 트래픽 기반 계산 필수
- ✅ 부하 테스트로 검증

**공식**:
```
필요 Connection 수 = (Peak RPS × 평균 처리 시간) + 여유분

예:
  Peak RPS = 500
  평균 처리 시간 = 0.05초 (50ms)
  필요 Connection = 500 × 0.05 = 25개
  여유분 100% → 50개
```

### 2. 비동기 처리의 중요성
- I/O 작업은 가능한 비동기로 처리
- Blocking 최소화로 처리량 향상
- Callback/Future를 활용한 에러 처리

### 3. 모니터링이 생명이다
- 수집하지 않으면 분석할 수 없다
- Connection Pool 같은 핵심 메트릭 필수
- 알람 설정으로 조기 감지

## 6.2 프로세스 교훈

### 1. 팀 간 커뮤니케이션이 핵심
- 마케팅 이벤트는 반드시 사전 공유
- 예상 트래픽, 피크 시간, 지속 시간 필수
- 정기적인 Sync-up 미팅

### 2. 부하 테스트는 현실적으로
- **예상 트래픽의 최소 2배**로 테스트
- 다양한 시나리오 커버 (Baseline, Spike, Stress)
- 프로덕션과 유사한 환경에서 수행

### 3. 장애 대응 매뉴얼 필수
- Runbook 작성 및 정기 업데이트
- 온보딩 시 필수 교육 자료
- 정기적인 장애 훈련 (Chaos Engineering)

## 6.3 조직 교훈

### 1. 예방이 최선이다
- 사후 대응보다 사전 예방이 비용 효율적
- 정기 리뷰 및 개선 문화
- 기술 부채 적극 관리

### 2. 실패는 학습 기회다
- Blameless Postmortem 문화 정착
- 재발 방지에 집중
- 전사 공유로 조직 학습

### 3. 지속적 개선
- 한 번의 개선으로 끝이 아님
- 정기적인 성능 테스트 및 모니터링
- 새로운 기술/패턴 적극 도입

---

# 7. 참고 자료

## 7.1 관련 문서
- [STEP19 부하 테스트 계획서](STEP19_LOAD_TEST_PLAN.md)
- [K6 테스트 결과](../k6/results/)
- [시스템 아키텍처 문서](../README.md)

## 7.2 외부 참조
- [Lettuce Connection Pool 설정](https://github.com/lettuce-io/lettuce-core/wiki/Connection-Pooling)
- [HikariCP Best Practices](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Redis Cluster Tutorial](https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/)

## 7.3 테스트 아티팩트
- K6 결과 JSON: `k6/results/baseline_*.json`, `k6/results/spike_*.json`
- 애플리케이션 로그: `logs/application_20251226.log`
- Redis 모니터링: `monitoring/redis_stats_20251226.txt`

---

# 8. 승인 및 리뷰

| 역할 | 이름 | 승인 | 날짜 | 코멘트 |
|------|------|------|------|--------|
| 작성자 | Backend Team | ✓ | 2025-12-26 | - |
| Backend 리드 | - | - | - | - |
| DevOps 리드 | - | - | - | - |
| CTO | - | - | - | - |

---

**문서 버전**: 1.0
**최종 수정일**: 2025-12-26
**다음 리뷰 예정일**: 2026-01-26 (1개월 후)
