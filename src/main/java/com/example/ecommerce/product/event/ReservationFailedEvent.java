package com.example.ecommerce.product.event;

import com.example.ecommerce.common.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 재고 또는 쿠폰 예약 실패 이벤트
 */
public record ReservationFailedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String aggregateType,
    Long aggregateId,
    Long orderId,
    String failureReason
) implements DomainEvent {

    public static ReservationFailedEvent forStock(Long orderId, String failureReason) {
        return new ReservationFailedEvent(
            UUID.randomUUID().toString(),
            "ReservationFailed",
            LocalDateTime.now(),
            "Order",
            orderId,
            orderId,
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
