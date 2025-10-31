package com.example.ecommerce.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상태")
public enum OrderStatus {
    @Schema(description = "결제 대기")
    PENDING,

    @Schema(description = "결제 완료")
    PAYMENT_COMPLETED,

    @Schema(description = "주문 취소")
    CANCELLED
}
