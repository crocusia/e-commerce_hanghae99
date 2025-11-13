package com.example.ecommerce.payment.dto;

import com.example.ecommerce.payment.domain.Payment;
import com.example.ecommerce.payment.domain.status.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "결제 응답")
public record PaymentResponse(
    @Schema(description = "결제 ID") Long paymentId,
    @Schema(description = "주문 ID") Long orderId,
    @Schema(description = "사용자 ID") Long userId,
    @Schema(description = "결제 금액") Long amount,
    @Schema(description = "결제 상태") PaymentStatus status,
    @Schema(description = "생성 일시") LocalDateTime createdAt,
    @Schema(description = "완료 일시") LocalDateTime completedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getCreatedAt(),
            payment.getCompletedAt()
        );
    }
}
