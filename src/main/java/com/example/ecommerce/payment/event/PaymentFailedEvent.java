package com.example.ecommerce.payment.event;

import com.example.ecommerce.common.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 실패 이벤트
 * 결제 처리 중 오류가 발생했을 때 발행
 */
public record PaymentFailedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String aggregateType,
    Long aggregateId,
    Long paymentId,
    Long orderId,
    Long userId,
    String failureReason
) implements DomainEvent {

    public static PaymentFailedEvent of(Long paymentId, Long orderId, Long userId, String failureReason) {
        return new PaymentFailedEvent(
            UUID.randomUUID().toString(),
            "PaymentFailed",
            LocalDateTime.now(),
            "Payment",
            paymentId,
            paymentId,
            orderId,
            userId,
            failureReason
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
