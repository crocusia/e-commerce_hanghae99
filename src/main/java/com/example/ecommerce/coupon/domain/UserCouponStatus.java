package com.example.ecommerce.coupon.domain;

public enum UserCouponStatus {
    UNUSED("미사용"),
    USED("사용됨"),
    EXPIRED("만료됨");

    private final String description;

    UserCouponStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
