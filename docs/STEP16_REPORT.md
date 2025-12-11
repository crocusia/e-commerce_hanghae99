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
**MSA 아키텍처 패턴 학습**
-   도메인 이벤트를 통한 관심사 분리로 서비스 간 느슨한 결합(Loose Coupling) 실현
  - Monolith에서 MSA로 전환 가능한 구조 설계, 추후 Message Queue로 전환 용이한 구조

---

## 현재 아키텍처 개요

### 아키텍처 다이어그램
- 코레오그래피 방식
- 주문, 결제, 재고, 쿠폰 도메인을 분리함.
- OrderCreationOrchestrator, PaymentOrchestrator는 이름만 Orchestrator이고 실제로는 이벤트 발행만 수행하고 있음

### 핵심 컴포넌트
#### 1. Event Listener
- **역할**: 도메인 이벤트 구독 및 처리
- **구현**:
  - `StockEventListener`: 재고 예약/확정/해제
  - `OrderEventListener`: 주문 상태 변경
  - `PaymentEventListener`: 결제 처리
  - `UserCouponEventListener`: 쿠폰 사용/복구
- **특징**:
  - `@TransactionalEventListener(phase = AFTER_COMMIT)`: 트랜잭션 분리
  - 독립적인 트랜잭션 실행 (`REQUIRES_NEW`)
  - `@Async`: 비동기 처리 (선택적)

#### 2. Domain Event
- **역할**: 도메인 변경 사항 전달
- **구현**:
  - `OrderCreatedEvent`: 주문 생성
  - `ReservationCompletedEvent` : 재고 예약 완료
  - `ReservationFailedEvent` : 재고 예약 완료
  - `PaymentCreatedEvent`: 결제 생성
  - `PaymentCompletedEvent`: 결제 완료
  - `PaymentFailedEvent`: 결제 실패
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
## 주문-재고-결제 플로우

### 주문 - 재고 - 결제 생성
```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant EventBus
    participant StockService
    participant PaymentService

    %% 1. 주문 생성 (동기)
    Client->>OrderService: POST /orders (주문하기)
    activate OrderService
    OrderService->>OrderService: 1. Order 생성 (status: PENDING_RESERVATION)
    OrderService->>EventBus: publish(OrderCreatedEvent)
    Note over OrderService: [TX 1 커밋]
    OrderService-->>Client: 
    deactivate OrderService

    Note over Client: 이후 모든 단계는 비동기 처리

    %% 2. 재고 예약 (비동기 - StockService)
    EventBus->>StockService: consume(OrderCreatedEvent)
    activate StockService
    StockService->>StockService: 2. 재고 예약
    
    alt 재고 부족
        StockService->>EventBus: publish(ReservationFailedEvent)
        Note over StockService: **재고 예약 실패**
    else 재고 성공
        StockService->>EventBus: publish(ReservationCompletedEvent)
        Note over StockService: **재고 예약 성공**
    end
    deactivate StockService
    
    %% 3.1. [실패] 재고 예약 실패 시 주문 취소
    EventBus->>OrderService: consume(ReservationFailedEvent)
    activate OrderService
    OrderService->>OrderService: 3.1. Order 상태 변경 (status: RESERVATION_FAILED)
    OrderService->>EventBus: publish(OrderStatusChangedEvent)
    deactivate OrderService

    %% 3.2. [성공] 재고 예약 완료 시 결제 시도
    EventBus->>OrderService: consume(ReservationCompletedEvent)
    activate OrderService
    OrderService->>OrderService: 3.2. Order 상태 변경 (status: PENDING)
    OrderService->>EventBus: publish(OrderPendingEvent)
    deactivate OrderService

    EventBus->>PaymentService: consume(OrderPendingEvent)
    activate PaymentService
    PaymentService->>PaymentService: 4. Payment 생성 및 결제 시도
```

### 결제
**결제 성공 시**
```
PaymentFailedEvent 발행
   ↓
StockEventListener.handlePaymentFailed()
   ↓
StockService.releaseReservation() (재고 예약 해제)

OrderEventListener.handlePaymentFailed()
   ↓
Order.status = CANCELLED (결제 실패로 인한 주문 실패)
```

```mermaid
sequenceDiagram
      participant EventBus
      participant PaymentListener
      participant StockListener
      participant StockService
      participant OrderListener
      participant CouponListener

      Note over EventBus: 주문 생성 → 재고 예약 완료<br/>→ 결제 자동 생성까지 성공

      EventBus->>PaymentListener: handlePaymentCreated(event)
      activate PaymentListener
      Note over PaymentListener: 결제 성공
      PaymentListener->>EventBus: publish(PaymentCompleteEvent)
      Note over PaymentListener: [TX5 커밋]
      deactivate PaymentListener

    par 주문 상태 최종 확정
        EventBus->>OrderListener: consume(PaymentCompletedEvent)
        activate OrderListener
        Note over OrderListener: status: PENDING → **PAYMENT_COMPLETED**
        Note over OrderListener: [TX 6A 커밋]
        deactivate OrderListener
    and 재고 최종 확정 (차감)
        EventBus->>StockListener: consume(PaymentCompletedEvent)
        activate StockService
        StockListener->>StockService: confirmReservation()
        Note over StockService: 예약 해제 & **실제 재고 차감**
        Note over StockService: [TX 6B 커밋]
        deactivate StockService
    and 쿠폰 최종 사용 처리
        EventBus->>CouponListener: consume(PaymentCompletedEvent)
        activate CouponListener
        Note over CouponListener: 임시 사용 → **최종 사용 처리**
        Note over CouponListener: [TX 6C 커밋]
        deactivate CouponListener
    end

```

**결제 실패 시**
```
PaymentFailedEvent 발행
   ↓
StockEventListener.handlePaymentFailed()
   ↓
StockService.releaseReservation() (재고 예약 해제)

OrderEventListener.handlePaymentFailed()
   ↓
Order.status = CANCELLED (결제 실패로 인한 주문 실패)
```

```mermaid
sequenceDiagram
      participant EventBus
      participant PaymentListener
      participant PaymentService
      participant StockListener
      participant StockService
      participant OrderListener
      participant CouponListener

      EventBus->>PaymentListener: handlePaymentCreated(event)
      activate PaymentListener
      PaymentListener->>PaymentService: processPayment(paymentId)
      activate PaymentService
      PaymentService->>PaymentListener: 잔액 차감<br/>(잔액 부족시)
      deactivate PaymentService

      PaymentListener->>EventBus: publish(PaymentFailedEvent)<br/>reason: "잔액 부족"
      Note over PaymentListener: [TX5 커밋]
      deactivate PaymentListener

      Note over EventBus: 보상 트랜잭션 시작<br/>(Compensating Transaction)

      par 주문 취소
          EventBus->>OrderListener: @Async @EventListener<br/>handlePaymentFailed(event)
          activate OrderListener
          OrderListener->>OrderListener: order.cancel()
          Note over OrderListener: status: PENDING → CANCELLED
          deactivate OrderListener
      and 재고 예약 해제
          EventBus->>StockListener: @Async @EventListener<br/>handlePaymentFailed(event)
          activate StockListener
          StockListener->>StockService: releaseReservation(orderId)
          activate StockService
          StockService->>StockService: 예약 해제<br/>재고 복구
          StockService-->>StockListener: success
          deactivate StockService
          deactivate StockListener
      and 쿠폰 복구 (사용했다면)
          EventBus->>CouponListener: @Async @EventListener<br/>handlePaymentFailed(event)
          activate CouponListener
          CouponListener->>CouponListener: 쿠폰 사용 취소
          deactivate CouponListener
      end

      Note over EventBus: 보상 트랜잭션 완료<br/>모든 리소스 원복

```
## 고민한 점
### 1. 데이터 일관성 및 사용자 경험 리스크

## MSA 구조의 장단점
### 1. 데이터 일관성 및 사용자 경험 리스크

## MSA 전환 고려사항

### 현재 → MSA 마이그레이션 로드맵

#### Phase 1: Monolith with Message Queue
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

#### Phase 2: Microservices
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
