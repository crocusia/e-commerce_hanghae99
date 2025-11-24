package com.example.ecommerce.common.outbox.domain;

import com.example.ecommerce.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "outbox", indexes = {
    @Index(name = "idx_outbox_status_created_at", columnList = "status, created_at"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Outbox extends BaseEntity {

    @Column(name = "event_id", unique = true, nullable = false, length = 36)  // ⭐ 추가
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;
    
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public static Outbox create(
        String eventId,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String payload
    ) {
        return Outbox.builder()
            .eventId(eventId)
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .payload(payload)
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .build();
    }
    
    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
    
    public boolean canRetry(int maxRetryCount) {
        return this.retryCount < maxRetryCount && this.status == OutboxStatus.FAILED;
    }

    public void resetForRetry() {
        if (this.status == OutboxStatus.FAILED) {
            this.status = OutboxStatus.PENDING;
        }
    }
}