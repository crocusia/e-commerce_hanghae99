package com.example.ecommerce.coupon.domain;

public enum DiscountType {
    FIXED("정액 할인"),
    PERCENTAGE("정률 할인");

    private final String description;

    DiscountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
