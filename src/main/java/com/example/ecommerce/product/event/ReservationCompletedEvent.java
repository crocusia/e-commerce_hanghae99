package com.example.ecommerce.product.event;

import com.example.ecommerce.common.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 재고 예약 완료 이벤트
 */
public record ReservationCompletedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String aggregateType,
    Long aggregateId,
    Long orderId
) implements DomainEvent {

    public static ReservationCompletedEvent of(Long orderId) {
        return new ReservationCompletedEvent(
            UUID.randomUUID().toString(),
            "ReservationCompleted",
            LocalDateTime.now(),
            "Order",
            orderId,
            orderId
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
