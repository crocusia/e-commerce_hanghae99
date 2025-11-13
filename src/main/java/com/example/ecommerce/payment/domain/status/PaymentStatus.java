package com.example.ecommerce.payment.domain.status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 상태 : 결제 대기, 결제 완료, 결제 실패")
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED
}