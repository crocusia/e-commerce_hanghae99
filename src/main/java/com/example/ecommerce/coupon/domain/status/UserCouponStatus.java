package com.example.ecommerce.coupon.domain.status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 쿠폰 상태 : 미사용, 예약됨, 사용됨, 만료됨")
public enum UserCouponStatus {
    UNUSED,
    RESERVED,
    USED,
    EXPIRED
}
