package com.example.ecommerce.payment.domain;

import com.example.ecommerce.payment.domain.status.PaymentStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Payment {
    private final Long id;
    private final Long orderId;
    private final Long userId;
    private final Long amount;
    private PaymentStatus status;
    private String failureReason;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    @Builder
    private Payment(Long id, Long orderId, Long userId, Long amount) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 결제입니다.");
        }
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }
}