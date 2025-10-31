package com.example.ecommerce.dto.coupon;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 쿠폰 상태")
public enum UserCouponStatus {
    @Schema(description = "미사용")
    UNUSED,

    @Schema(description = "사용 완료")
    USED,

    @Schema(description = "만료")
    EXPIRED
}
