package com.example.ecommerce.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "쿠폰 발급 요청")
public record IssueCouponRequest(
    @Schema(description = "사용자 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "사용자 ID는 필수입니다")
    @Positive(message = "사용자 ID는 양수여야 합니다")
    Long userId,

    @Schema(description = "쿠폰 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "쿠폰 ID는 필수입니다")
    @Positive(message = "쿠폰 ID는 양수여야 합니다")
    Long couponId
) {
}