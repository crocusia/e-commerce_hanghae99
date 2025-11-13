package com.example.ecommerce.order.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.order.domain.status.OrderStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Order {
    private final Long id;
    private final Long userId;
    private Long userCouponId;
    private Long totalAmount;
    private Long discountAmount;
    private Long finalAmount;
    private OrderStatus status;
    private List<OrderItem> orderItems;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Order(Long id, Long userId, Long userCouponId, Long discountAmount,
        List<OrderItem> orderItems) {

        this.id = id;
        this.userId = userId;
        this.userCouponId = userCouponId;
        this.orderItems = new ArrayList<>(orderItems);
        this.totalAmount = calculateTotalAmount(orderItems);

        // 할인 금액 처리
        if (discountAmount != null && discountAmount > 0) {
            this.discountAmount = discountAmount;
            this.finalAmount = this.totalAmount - discountAmount;
        } else {
            this.discountAmount = 0L;
            this.finalAmount = this.totalAmount;
        }

        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Order create(Long userId, List<OrderItem> orderItems) {
        return Order.builder()
            .userId(userId)
            .orderItems(orderItems)
            .build();
    }

    public void applyCoupon(Long userCouponId, Long discountAmount){
        if(this.status != OrderStatus.PENDING){
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_APPLY_COUPON);
        }

        applyDiscount(discountAmount);

        this.userCouponId = userCouponId;
    }

    private void applyDiscount(Long discountAmount) {
        if (discountAmount == null || discountAmount < 0) {
            throw new IllegalArgumentException("할인 금액은 0 이상이어야 합니다.");
        }
        if (discountAmount > this.totalAmount) {
            throw new IllegalArgumentException("할인 금액이 총 주문 금액보다 클 수 없습니다.");
        }
        this.discountAmount = discountAmount;
        this.finalAmount = this.totalAmount - discountAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public void completePayment() {
        this.status = OrderStatus.PAYMENT_COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public List<OrderItem> getOrderItems() {
        return new ArrayList<>(orderItems);
    }

    private Long calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
            .mapToLong(OrderItem::getSubtotal)
            .sum();
    }
}
