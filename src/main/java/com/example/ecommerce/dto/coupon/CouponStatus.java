package com.example.ecommerce.dto.coupon;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 상태")
public enum CouponStatus {
    @Schema(description = "활성")
    ACTIVE,

    @Schema(description = "비활성")
    INACTIVE,

    @Schema(description = "삭제")
    DELETED
}
