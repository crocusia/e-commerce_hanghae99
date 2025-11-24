package com.example.ecommerce.payment.dto;

import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.payment.domain.Payment;
import com.example.ecommerce.user.domain.User;
import lombok.Builder;

@Builder
public record PaymentResult(
    boolean success,
    Payment payment,
    Order order,
    Long userId,
    Long finalAmount,
    String failureReason
) {
    public static PaymentResult success(Payment payment, Order order, User user) {
        return PaymentResult.builder()
            .success(true)
            .payment(payment)
            .order(order)
            .userId(user.getId())
            .finalAmount(order.getFinalAmount())
            .failureReason(null)
            .build();
    }

    public static PaymentResult failure(Payment payment, Order order, String reason) {
        return PaymentResult.builder()
            .success(false)
            .payment(payment)
            .order(order)
            .userId(payment.getUserId())
            .finalAmount(order.getFinalAmount())
            .failureReason(reason)
            .build();
    }
}
