package com.example.ecommerce.dto.coupon;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "사용자 쿠폰 응답")
public record UserCouponResponse(
    @Schema(description = "사용자 쿠폰 ID")
    Long id,

    @Schema(description = "사용자 ID")
    Long userId,

    @Schema(description = "쿠폰 ID")
    Long couponId,

    @Schema(description = "쿠폰명")
    String couponName,

    @Schema(description = "할인 금액")
    Long discountPrice,

    @Schema(description = "할인율")
    Double discountRate,

    @Schema(description = "최소 주문 금액")
    Long minOrderAmount,

    @Schema(description = "유효 시작일")
    LocalDate validFrom,

    @Schema(description = "유효 종료일")
    LocalDate validUntil,

    @Schema(description = "발급 일시")
    LocalDateTime issuedAt,

    @Schema(description = "사용 일시")
    LocalDateTime usedAt,

    @Schema(description = "사용된 주문 ID")
    Long orderId,

    @Schema(description = "쿠폰 상태")
    UserCouponStatus status
) {
}
