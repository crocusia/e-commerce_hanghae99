package com.example.ecommerce.coupon.domain.status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 상태 : 활성, 비활성, 삭제됨")
public enum CouponStatus {
    ACTIVE,
    INACTIVE,
    DELETED
}
