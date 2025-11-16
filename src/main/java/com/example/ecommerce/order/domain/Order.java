package com.example.ecommerce.order.domain;

import com.example.ecommerce.common.domain.BaseEntity;
import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.order.domain.status.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_coupon_id")
    private Long userCouponId;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "final_amount", nullable = false)
    private Long finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    public static Order create(Long userId, List<OrderItem> orderItems) {
        Long totalAmount = calculateTotalAmount(orderItems);

        Order order = Order.builder()
            .userId(userId)
            .totalAmount(totalAmount)
            .discountAmount(0L)
            .finalAmount(totalAmount)
            .status(OrderStatus.PENDING)
            .build();

        orderItems.forEach(order::addOrderItem);
        return order;
    }

    public void addOrderItem(OrderItem orderItem) {
        if (this.orderItems == null) {
            this.orderItems = new ArrayList<>();
        }
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
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
    }

    public void completePayment() {
        this.status = OrderStatus.PAYMENT_COMPLETED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public List<OrderItem> getOrderItems() {
        return new ArrayList<>(orderItems);
    }

    private static Long calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
            .mapToLong(OrderItem::getSubtotal)
            .sum();
    }
}
