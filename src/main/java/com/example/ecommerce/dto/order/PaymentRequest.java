package com.example.ecommerce.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "결제 요청")
public record PaymentRequest(
    @Schema(description = "주문 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "주문 ID는 필수입니다")
    @Positive(message = "주문 ID는 양수여야 합니다")
    Long orderId,

    @Schema(description = "쿠폰 ID (선택사항)")
    @Positive(message = "쿠폰 ID는 양수여야 합니다")
    Long userCouponId
) {
}
