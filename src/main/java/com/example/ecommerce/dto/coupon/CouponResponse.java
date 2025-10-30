package com.example.ecommerce.dto.coupon;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "쿠폰 응답")
public record CouponResponse(
    @Schema(description = "쿠폰 ID")
    Long id,

    @Schema(description = "쿠폰명")
    String name,

    @Schema(description = "할인 금액")
    Long discountPrice,

    @Schema(description = "할인율")
    Double discountRate,

    @Schema(description = "총 발급 수량")
    Integer totalQuantity,

    @Schema(description = "발급된 수량")
    Integer issuedQuantity,

    @Schema(description = "남은 수량")
    Integer remainingQuantity,

    @Schema(description = "유효 시작일")
    LocalDate validFrom,

    @Schema(description = "유효 종료일")
    LocalDate validUntil,

    @Schema(description = "최소 주문 금액")
    Integer minOrderAmount,

    @Schema(description = "쿠폰 상태")
    CouponStatus status
) {
}
