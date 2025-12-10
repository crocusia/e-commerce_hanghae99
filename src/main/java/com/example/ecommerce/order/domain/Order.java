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

    @jakarta.persistence.Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    public static Order create(Long userId, List<OrderItem> orderItems) {
        Long totalAmount = calculateTotalAmount(orderItems);

        Order order = Order.builder()
            .userId(userId)
            .totalAmount(totalAmount)
            .discountAmount(0L)
            .finalAmount(totalAmount)
            .status(OrderStatus.PENDING_RESERVATION)
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
        if(this.status != OrderStatus.PENDING && this.status != OrderStatus.PENDING_RESERVATION){
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_APPLY_COUPON);
        }

        applyDiscount(discountAmount);

        this.userCouponId = userCouponId;
    }

    public Long cancelCoupon() {
        if (this.userCouponId == null) {
            return null;
        }

        Long previousCouponId = this.userCouponId;
        this.userCouponId = null;
        this.discountAmount = 0L;
        this.finalAmount = this.totalAmount;

        return previousCouponId;
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

    public void validateForPayment() {
        if (this.status != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_PROCESS_PAYMENT);
        }
    }

    public void completeReservation() {
        if (this.status != OrderStatus.PENDING_RESERVATION) {
            throw new IllegalStateException("예약 완료 처리는 PENDING_RESERVATION 상태에서만 가능합니다.");
        }
        this.status = OrderStatus.PENDING;
    }

    public void completePayment() {
        this.status = OrderStatus.PAYMENT_COMPLETED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public void failReservation() {
        if (this.status != OrderStatus.PENDING_RESERVATION) {
            throw new IllegalStateException("예약 실패 처리는 PENDING_RESERVATION 상태에서만 가능합니다.");
        }
        this.status = OrderStatus.RESERVATION_FAILED;
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
