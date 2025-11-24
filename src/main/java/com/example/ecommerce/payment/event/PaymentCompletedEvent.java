package com.example.ecommerce.payment.event;

import com.example.ecommerce.common.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 완료 이벤트
 * 결제가 성공적으로 완료되었을 때 발행
 */
public record PaymentCompletedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String aggregateType,
    Long aggregateId,
    Long paymentId,
    Long orderId,
    Long userId,
    Long amount
) implements DomainEvent {

    public static PaymentCompletedEvent of(Long paymentId, Long orderId, Long userId, Long amount) {
        return new PaymentCompletedEvent(
            UUID.randomUUID().toString(),
            "PaymentCompleted",
            LocalDateTime.now(),
            "Payment",
            paymentId,
            paymentId,
            orderId,
            userId,
            amount
        );
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public Long getAggregateId() {
        return aggregateId;
    }
}
