package com.example.ecommerce.coupon.domain.status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "할인 유형 : 정액 할인, 정률 할인")
public enum DiscountType {
    FIXED,
    PERCENTAGE
}
