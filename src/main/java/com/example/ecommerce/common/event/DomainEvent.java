package com.example.ecommerce.common.event;

import java.time.LocalDateTime;

public interface DomainEvent {
    String getEventId();
    String getEventType();
    LocalDateTime getOccurredAt();
    String getAggregateType();
    Long getAggregateId();
}
