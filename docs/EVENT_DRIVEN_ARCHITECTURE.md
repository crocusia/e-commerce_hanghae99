# 이벤트 기반 아키텍처 설계 문서

## 📋 목차
- [배경 및 목적](#배경-및-목적)
- [현재 아키텍처 개요](#현재-아키텍처-개요)
- [설계 결정 사항 (ADR)](#설계-결정-사항-adr)
- [주문-재고-결제 플로우](#주문-재고-결제-플로우)
- [트랜잭션 경계 및 처리 전략](#트랜잭션-경계-및-처리-전략)
- [장단점 분석](#장단점-분석)
- [MSA 전환 고려사항](#msa-전환-고려사항)

---

## 배경 및 목적

### 학습 목표
1. **MSA 아키텍처 패턴 학습**
   - Monolith에서 MSA로 전환 가능한 구조 설계
   - 서비스 간 느슨한 결합(Loose Coupling) 실현
   - 도메인 이벤트를 통한 관심사 분리

2. **이벤트 기반 설계 경험**
   - 동기 호출의 강결합 문제 해결
   - 비즈니스 로직과 부가 로직 분리
   - 확장 가능한 아키텍처 구축

3. **실무 적용 가능성 검증**
   - Kafka 없이도 이벤트 기반 설계 가능
   - Spring Events를 활용한 점진적 개선
   - 추후 Message Queue로 전환 용이한 구조

---

## 현재 아키텍처 개요

### 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                     Monolithic Application                   │
│                                                               │
│  ┌──────────────┐     ┌──────────────┐    ┌──────────────┐  │
│  │   Order      │     │   Stock      │    │   Payment    │  │
│  │  Service     │     │   Service    │    │   Service    │  │
│  └──────┬───────┘     └──────┬───────┘    └──────┬───────┘  │
│         │                    │                   │           │
│  ┌──────▼───────────────────▼───────────────────▼───────┐   │
│  │           Spring Application Event Bus             │   │
│  └──────┬───────────────────┬───────────────────┬───────┘   │
│         │                    │                   │           │
│  ┌──────▼───────┐     ┌──────▼───────┐    ┌──────▼───────┐  │
│  │ OrderEvent   │     │ StockEvent   │    │PaymentEvent  │  │
│  │  Listener    │     │  Listener    │    │  Listener    │  │
│  └──────────────┘     └──────────────┘    └──────────────┘  │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │          Orchestrator (Saga Coordinator)            │    │
│  │  - OrderCreationOrchestrator                        │    │
│  │  - PaymentOrchestrator                              │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 컴포넌트

#### 1. Orchestrator (Saga Coordinator)
- **역할**: 비즈니스 플로우 조율 및 이벤트 발행
- **구현**:
  - `OrderCreationOrchestrator`: 주문 생성 플로우 관리
  - `PaymentOrchestrator`: 결제 생성 플로우 관리
- **책임**:
  - 트랜잭션 경계 설정
  - 도메인 이벤트 발행
  - 플로우 로깅 및 모니터링

#### 2. Event Listener
- **역할**: 도메인 이벤트 구독 및 처리
- **구현**:
  - `StockEventListener`: 재고 예약/확정/해제
  - `OrderEventListener`: 주문 상태 변경
  - `PaymentEventListener`: 결제 처리
  - `UserCouponEventListener`: 쿠폰 사용/복구
- **특징**:
  - `@TransactionalEventListener(phase = AFTER_COMMIT)`: 트랜잭션 분리
  - `@Async`: 비동기 처리 (선택적)
  - 독립적인 트랜잭션 실행 (`REQUIRES_NEW`)

#### 3. Domain Event
- **역할**: 도메인 변경 사항 전달
- **구현 예시**:
  ```java
  public record OrderCreatedEvent(
      String eventId,
      String eventType,
      LocalDateTime occurredAt,
      String aggregateType,
      Long aggregateId,
      // ... 도메인 데이터
  ) implements DomainEvent { }
  ```

---

## 설계 결정 사항 (ADR)

### Decision 1: Spring Events 사용 (Kafka 미사용)

**상황**
- 초기 학습 단계에서 이벤트 기반 아키텍처 도입 필요
- Kafka 인프라 없이 빠른 프로토타이핑 원함

**결정**
Spring의 `ApplicationEventPublisher`와 `@TransactionalEventListener` 사용

**근거**
1. ✅ **학습 곡선 감소**: Kafka 설치/설정 없이 즉시 사용 가능
2. ✅ **트랜잭션 통합**: Spring 트랜잭션과 자연스러운 통합
3. ✅ **점진적 개선**: 추후 Kafka로 쉽게 마이그레이션 가능
4. ✅ **단순성**: 인메모리 이벤트 버스로 디버깅 용이

**트레이드오프**
- ❌ 이벤트 영속성 없음 (앱 재시작 시 유실)
- ❌ 분산 시스템에서 사용 불가 (동일 JVM 내에서만 동작)
- ❌ 이벤트 순서 보장 제한적
- ❌ 재시도 메커니즘 직접 구현 필요

**대안**
- Kafka: 프로덕션 환경에서 권장
- RabbitMQ: 메시지 라우팅이 복잡한 경우
- AWS SNS/SQS: 클라우드 환경

---

### Decision 2: Saga Orchestrator 패턴

**상황**
- 주문-재고-결제의 분산 트랜잭션 처리 필요
- Choreography vs Orchestration 선택

**결정**
Orchestrator 패턴 채택 (`OrderCreationOrchestrator`, `PaymentOrchestrator`)

**근거**
1. ✅ **플로우 가시성**: 중앙 집중식으로 비즈니스 플로우 파악 용이
2. ✅ **보상 트랜잭션 관리**: 실패 시 rollback 로직 명확화
3. ✅ **디버깅 편의성**: 로그 추적이 쉬움
4. ✅ **MSA 전환 준비**: Saga Orchestrator는 MSA에서도 유효한 패턴

**트레이드오프**
- ❌ Orchestrator가 단일 장애점(SPOF)이 될 수 있음
- ❌ 서비스 간 의존성 존재 (완전한 분리는 아님)

**대안**
- Choreography: 서비스가 완전히 독립적이고 복잡도가 낮은 경우

---

### Decision 3: 트랜잭션 분리 전략

**상황**
- 주문 생성과 재고 예약은 독립적인 트랜잭션으로 처리 필요
- 재고 실패 시 주문은 이미 커밋된 상태

**결정**
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

**근거**
1. ✅ **트랜잭션 격리**: 각 서비스의 트랜잭션 독립성 보장
2. ✅ **성능**: 긴 트랜잭션 방지
3. ✅ **장애 격리**: 한 서비스 실패가 다른 서비스에 영향 최소화
4. ✅ **이벤트 일관성**: `AFTER_COMMIT`으로 이벤트는 커밋 후에만 발행

**트레이드오프**
- ❌ 최종 일관성(Eventual Consistency): 즉시 일관성 보장 안 됨
- ❌ 보상 트랜잭션 필요: 실패 시 명시적 rollback 구현

---

### Decision 4: 비동기 처리 (@Async)

**상황**
- 일부 이벤트는 응답 시간에 영향을 주지 않아야 함
- 예: 결제 완료 후 재고 확정, 데이터 플랫폼 전송

**결정**
선택적 `@Async` 적용
- 동기: 재고 예약 (주문 생성 플로우의 필수 단계)
- 비동기: 재고 확정, 데이터 플랫폼 전송 (부가 로직)

**근거**
1. ✅ **응답 시간 개선**: 불필요한 대기 제거
2. ✅ **장애 격리**: 부가 로직 실패가 핵심 플로우에 영향 없음
3. ✅ **스레드 풀 활용**: 병렬 처리로 처리량 향상

**트레이드오프**
- ❌ 디버깅 복잡도: 비동기 스택 트레이스 추적 어려움
- ❌ 스레드 안전성: 멀티스레드 환경 고려 필요

---

## 주문-재고-결제 플로우

### 전체 시퀀스

```
Client → Controller → Orchestrator → Service → Event Bus → Listener

[1. 주문 생성]
POST /orders
   ↓
OrderCreationOrchestrator.createOrder()
   ↓
OrderService.createOrderEntity() [TX1 시작]
   ↓
Order 저장 → OrderCreatedEvent 발행
   ↓
[TX1 커밋] ━━━━━━━━━━━━━━━━━━━━━━ (트랜잭션 경계)
   ↓
StockEventListener.handleOrderCreated() [TX2 시작, 동기]
   ↓
StockService.reserve() (각 상품별)
   ├─ 성공 → ReservationCompletedEvent 발행
   └─ 실패 → ReservationFailedEvent 발행
   ↓
[TX2 커밋]
   ↓
OrderEventListener.handleReservation{Completed|Failed}() [TX3]
   ↓
Order.status 업데이트 (PENDING or RESERVATION_FAILED)

[2. 결제 요청]
POST /payments
   ↓
PaymentOrchestrator.createPayment()
   ↓
PaymentService.createPayment() [TX4]
   ↓
Payment 저장 → PaymentCreatedEvent 발행
   ↓
[TX4 커밋]
   ↓
PaymentEventListener.handlePaymentCreated() [TX5]
   ↓
외부 PG 호출
   ├─ 성공 → PaymentCompletedEvent 발행
   └─ 실패 → PaymentFailedEvent 발행
   ↓
[병렬 처리 - @Async]
├─ StockEventListener.handlePaymentCompleted()
│     → StockService.confirmReservation() (재고 확정)
│
├─ OrderEventListener.handlePaymentCompleted()
│     → Order.status = PAYMENT_COMPLETED
│
└─ UserCouponEventListener.handlePaymentFailed() (결제 실패 시)
      → Coupon 복구
```

### 트랜잭션 격리 예시

```java
// TX1: 주문 생성 (OrderCreationOrchestrator)
@Transactional
public OrderResponse createOrder(OrderRequest request) {
    Order order = orderService.createOrderEntity(request);  // DB 저장
    eventPublisher.publish(OrderCreatedEvent.from(order));  // 이벤트 발행
    return OrderResponse.from(order);
} // TX1 커밋 → 이벤트 리스너 트리거

// TX2: 재고 예약 (StockEventListener)
@TransactionalEventListener(phase = AFTER_COMMIT)  // TX1 커밋 후 실행
@Transactional(propagation = REQUIRES_NEW)          // 새 트랜잭션
public void handleOrderCreated(OrderCreatedEvent event) {
    stockService.reserve(...);  // 독립적인 트랜잭션
}
```

### 보상 트랜잭션 (Compensating Transaction)

**재고 예약 실패 시**
```
ReservationFailedEvent 발행
   ↓
OrderEventListener.handleReservationFailed()
   ↓
Order.status = RESERVATION_FAILED (주문 취소 상태)
```

**결제 실패 시**
```
PaymentFailedEvent 발행
   ↓
StockEventListener.handlePaymentFailed()
   ↓
StockService.releaseReservation() (예약 해제)
   ↓
OrderEventListener.handlePaymentFailed()
   ↓
Order.status = CANCELLED
```

---

## 트랜잭션 경계 및 처리 전략

### 트랜잭션 전파 레벨 선택

| 상황 | 전파 레벨 | 이유 |
|------|-----------|------|
| Orchestrator | `REQUIRED` (기본값) | 새 트랜잭션 시작 |
| EventListener (동기) | `REQUIRES_NEW` | 독립적인 트랜잭션 필요 |
| EventListener (비동기) | `REQUIRES_NEW` | 별도 스레드에서 실행 |

### 이벤트 발행 시점

```java
// ❌ 잘못된 예: 트랜잭션 커밋 전 이벤트 발행
@Transactional
public void createOrder() {
    orderRepository.save(order);
    eventPublisher.publish(event);  // TX 롤백 시에도 이벤트 발행됨!
}

// ✅ 올바른 예: @TransactionalEventListener 사용
@Transactional
public void createOrder() {
    orderRepository.save(order);
    eventPublisher.publish(event);  // 이벤트만 등록
}

@TransactionalEventListener(phase = AFTER_COMMIT)  // 커밋 후 실행 보장
public void handleEvent(Event event) { ... }
```

### 멱등성(Idempotency) 보장

**문제**: 이벤트가 중복 발행되면?

**해결책**:
1. Event ID 기반 중복 체크
2. 도메인 상태로 중복 처리 방지

```java
@TransactionalEventListener
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    Order order = orderRepository.findById(event.orderId());

    // 이미 처리된 경우 무시 (멱등성)
    if (order.getStatus() == PAYMENT_COMPLETED) {
        log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
        return;
    }

    order.completePayment();
}
```

---

## 장단점 분석

### 장점

#### 1. 관심사 분리 (Separation of Concerns)
- 주문 서비스는 재고/결제 로직을 몰라도 됨
- 새로운 이벤트 리스너 추가가 기존 코드에 영향 없음
- 예: 데이터 플랫폼 전송 리스너 추가 (STEP 15)

#### 2. 확장성 (Scalability)
- 비동기 이벤트는 병렬 처리 가능
- 스레드 풀 설정으로 성능 조절

#### 3. 유연성 (Flexibility)
- 이벤트 구독자 추가/제거 용이
- 비즈니스 요구사항 변경 시 유연한 대응

#### 4. 테스트 용이성
- 각 리스너를 독립적으로 단위 테스트 가능
- Mock 이벤트로 통합 테스트

#### 5. MSA 전환 준비
- 이벤트 인터페이스는 그대로 유지
- 구현만 Kafka로 교체하면 됨

### 단점

#### 1. 복잡도 증가
- 단순 CRUD보다 코드 양 증가
- 이벤트 플로우 추적 어려움
- 디버깅 시 여러 클래스를 오가며 확인 필요

#### 2. 최종 일관성 (Eventual Consistency)
- 즉시 일관성 보장 안 됨
- 사용자에게 "처리 중" 상태 노출 필요

#### 3. 장애 처리 복잡도
- 보상 트랜잭션 명시적 구현 필요
- 부분 실패 시나리오 고려

#### 4. 성능 오버헤드
- 이벤트 발행/구독 비용
- 여러 트랜잭션으로 인한 DB 커넥션 증가

#### 5. Spring Events의 한계
- 단일 JVM 내에서만 동작
- 이벤트 영속성 없음 (재시작 시 유실)
- 순서 보장 제한적

---

## MSA 전환 고려사항

### 현재 → MSA 마이그레이션 로드맵

#### Phase 1: Monolith with Events (현재)
```
┌────────────────────────────────────┐
│      Single Application (JVM)      │
│  ┌─────────────────────────────┐   │
│  │   Spring Application Events │   │
│  └─────────────────────────────┘   │
└────────────────────────────────────┘
```

#### Phase 2: Monolith with Message Queue
```
┌────────────────────────────────────┐
│      Single Application (JVM)      │
│  ┌─────────────────────────────┐   │
│  │        Kafka / RabbitMQ     │ ◄─┼─ 외부 Message Broker
│  └─────────────────────────────┘   │
└────────────────────────────────────┘
```
**변경 사항**:
- `MessagePublisher` 구현을 Kafka Producer로 교체
- `@TransactionalEventListener` → Kafka Consumer로 변경
- 이벤트 영속성 확보

#### Phase 3: Microservices
```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Order Service│    │ Stock Service│    │Payment Service│
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           ↓
                    ┌─────────────┐
                    │    Kafka    │
                    └─────────────┘
```
**변경 사항**:
- 각 서비스를 별도 애플리케이션으로 분리
- 독립적인 DB (Database per Service)
- API Gateway 추가
- Service Discovery (Eureka, Consul 등)

### 코드 변경 최소화 전략

#### 1. 인터페이스 기반 설계
```java
// 현재: Spring Events
public interface MessagePublisher {
    void publish(DomainEvent event);
}

@Component
class SpringEventPublisher implements MessagePublisher {
    private final ApplicationEventPublisher publisher;

    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}

// MSA 전환 시: Kafka
@Component
class KafkaEventPublisher implements MessagePublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    public void publish(DomainEvent event) {
        kafkaTemplate.send("order-events", event);
    }
}
```

#### 2. 도메인 이벤트 표준화
```java
// 변경 없이 그대로 사용 가능
public interface DomainEvent {
    String getEventId();
    String getEventType();
    LocalDateTime getOccurredAt();
    String getAggregateType();
    Long getAggregateId();
}
```

#### 3. Saga Orchestrator 유지
- Orchestrator는 MSA에서도 유효한 패턴
- 단, REST API 호출 또는 메시지 발행으로 변경

### MSA 전환 시 고려사항

#### 1. 분산 트랜잭션
- 2PC는 성능 문제로 비권장
- Saga 패턴 (Orchestration 또는 Choreography)
- 최종 일관성 (Eventual Consistency) 수용

#### 2. 데이터 일관성
- 각 서비스가 독립적인 DB 보유
- CQRS 패턴으로 읽기 모델 분리 고려
- Event Sourcing으로 이벤트 이력 관리

#### 3. 장애 격리 (Fault Isolation)
- Circuit Breaker (Resilience4j, Hystrix)
- Retry 메커니즘
- Dead Letter Queue

#### 4. 모니터링 및 추적
- 분산 트레이싱 (Zipkin, Jaeger)
- 중앙 집중 로깅 (ELK Stack)
- 메트릭 수집 (Prometheus, Grafana)

---

## 결론

### 학습 성과
1. ✅ 이벤트 기반 아키텍처의 기본 개념 습득
2. ✅ Saga Orchestrator 패턴 실습
3. ✅ 트랜잭션 분리 및 보상 트랜잭션 구현
4. ✅ 비동기 처리 및 성능 최적화 경험
5. ✅ MSA 전환 시뮬레이션

### 실무 적용 가능성
- **단기**: Spring Events로 충분한 경우 (단일 서버, 낮은 트래픽)
- **중기**: Message Queue 도입 (확장성, 안정성 필요)
- **장기**: MSA 전환 (서비스 독립성, 팀 분리)

### Next Steps
1. Kafka/RabbitMQ 도입 검토
2. Event Sourcing 패턴 학습
3. CQRS 패턴 적용 실습
4. 분산 트레이싱 시스템 구축
5. Saga 보상 트랜잭션 고도화

---

## 참고 자료

### 패턴
- [Saga Pattern - Microservices.io](https://microservices.io/patterns/data/saga.html)
- [Event-Driven Architecture - Martin Fowler](https://martinfowler.com/articles/201701-event-driven.html)
- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)

### Spring Framework
- [Spring Events Documentation](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [@Async Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)

### Books
- "Building Microservices" by Sam Newman
- "Microservices Patterns" by Chris Richardson
- "Enterprise Integration Patterns" by Gregor Hohpe

---

**작성일**: 2025-12-11
**작성자**: E-Commerce Development Team
**버전**: 1.0
