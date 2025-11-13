package com.example.ecommerce.product.domain.status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재고 예약 상태: 예약중, 확정, 해제")
public enum ReservationStatus {
    RESERVED,
    CONFIRMED,
    RELEASED
}
