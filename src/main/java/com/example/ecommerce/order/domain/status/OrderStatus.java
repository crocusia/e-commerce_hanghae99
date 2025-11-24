package com.example.ecommerce.order.domain.status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상태")
public enum OrderStatus {
    @Schema(description = "예약 대기")
    PENDING_RESERVATION,

    @Schema(description = "결제 대기")
    PENDING,

    @Schema(description = "예약 실패")
    RESERVATION_FAILED,

    @Schema(description = "결제 완료")
    PAYMENT_COMPLETED,

    @Schema(description = "주문 취소")
    CANCELLED
}
