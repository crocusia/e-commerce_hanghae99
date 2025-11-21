package com.example.ecommerce.payment.event;

import com.example.ecommerce.common.event.DomainEvent;
import com.example.ecommerce.payment.domain.Payment;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCreatedEvent(
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

    public static PaymentCreatedEvent from(Payment payment) {
        return new PaymentCreatedEvent(
            UUID.randomUUID().toString(),
            "PaymentCreated",
            LocalDateTime.now(),
            "Payment",
            payment.getId(),
            payment.getId(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getAmount()
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
