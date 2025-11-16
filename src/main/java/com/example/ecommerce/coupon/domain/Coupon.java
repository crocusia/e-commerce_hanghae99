package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.domain.SoftDeleteEntity;
import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.status.CouponStatus;
import com.example.ecommerce.coupon.domain.vo.CouponQuantity;
import com.example.ecommerce.coupon.domain.vo.DiscountValue;
import com.example.ecommerce.coupon.domain.vo.ValidPeriod;
import com.example.ecommerce.product.domain.vo.Money;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Coupon extends SoftDeleteEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "discountType", column = @Column(name = "discount_type", nullable = false)),
        @AttributeOverride(name = "discountPrice", column = @Column(name = "discount_price")),
        @AttributeOverride(name = "discountRate", column = @Column(name = "discount_rate"))
    })
    private DiscountValue discountValue;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "totalQuantity", column = @Column(name = "total_quantity", nullable = false)),
        @AttributeOverride(name = "issuedQuantity", column = @Column(name = "issued_quantity", nullable = false))
    })
    private CouponQuantity quantity;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "validFrom", column = @Column(name = "valid_from", nullable = false)),
        @AttributeOverride(name = "validUntil", column = @Column(name = "valid_until", nullable = false))
    })
    private ValidPeriod validPeriod;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "min_order_amount", nullable = false))
    private Money minOrderAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status;

    // 정액 할인 쿠폰 생성
    public static Coupon createFixed(String name, Long discountPrice, int totalQuantity,
        LocalDateTime validFrom, LocalDateTime validUntil, Long minOrderAmount) {
        return Coupon.builder()
            .name(name)
            .discountValue(DiscountValue.fixed(discountPrice))
            .quantity(CouponQuantity.of(totalQuantity))
            .validPeriod(ValidPeriod.of(validFrom, validUntil))
            .minOrderAmount(Money.of(minOrderAmount))
            .status(CouponStatus.ACTIVE)
            .build();
    }

    // 정률 할인 쿠폰 생성
    public static Coupon createPercentage(String name, Double discountRate, int totalQuantity,
        LocalDateTime validFrom, LocalDateTime validUntil, Long minOrderAmount) {
        return Coupon.builder()
            .name(name)
            .discountValue(DiscountValue.percentage(discountRate))
            .quantity(CouponQuantity.of(totalQuantity))
            .validPeriod(ValidPeriod.of(validFrom, validUntil))
            .minOrderAmount(Money.of(minOrderAmount))
            .status(CouponStatus.ACTIVE)
            .build();
    }

    public void issue() {
        if (this.status != CouponStatus.ACTIVE) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (!this.validPeriod.isValid()) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }

        this.quantity = this.quantity.issue();
    }

    public boolean canIssue() {
        return this.status == CouponStatus.ACTIVE
            && this.validPeriod.isValid()
            && this.quantity.canIssue();
    }

    // 유효기간 확인
    public boolean isValid() {
        return this.validPeriod.isValid();
    }

    // 할인 금액 계산
    public Money calculateDiscountAmount(Money orderAmount) {
        // 최소 주문 금액 확인
        if (orderAmount.isLessThan(minOrderAmount)) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        return discountValue.calculateDiscountAmount(orderAmount);
    }

    // 상태 변경
    public void activate() {
        if (status == CouponStatus.DELETED) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.status = CouponStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = CouponStatus.INACTIVE;
    }

    @Override
    public void delete() {
        this.status = CouponStatus.DELETED;
        super.delete();
    }

}
