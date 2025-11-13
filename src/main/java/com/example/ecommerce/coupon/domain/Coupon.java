package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.status.CouponStatus;
import com.example.ecommerce.coupon.domain.vo.CouponQuantity;
import com.example.ecommerce.coupon.domain.vo.DiscountValue;
import com.example.ecommerce.coupon.domain.vo.ValidPeriod;
import com.example.ecommerce.product.domain.vo.Money;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Coupon {
    private final Long id;
    private String name;
    private DiscountValue discountValue;    // 할인 값 (타입 + 금액/비율)
    private CouponQuantity quantity;        // 쿠폰 수량 (총 수량 + 발급 수량)
    private ValidPeriod validPeriod;        // 유효 기간
    private Money minOrderAmount;
    private CouponStatus status;
    private LocalDateTime deletedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Coupon(Long id, String name, DiscountValue discountValue, CouponQuantity quantity, ValidPeriod validPeriod, Money minOrderAmount){
        this.id = id;
        this.name = name;
        this.discountValue = discountValue;
        this.quantity = quantity;
        this.validPeriod = validPeriod;
        this.minOrderAmount = minOrderAmount;
        this.status = CouponStatus.ACTIVE;
        this.deletedAt = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 정액 할인 쿠폰 생성
    public static Coupon createFixed(String name, Long discountPrice, int totalQuantity,
        LocalDate validFrom, LocalDate validUntil, Long minOrderAmount) {
        return Coupon.builder()
            .name(name)
            .discountValue(DiscountValue.fixed(Money.of(discountPrice)))
            .quantity(CouponQuantity.of(totalQuantity))
            .validPeriod(ValidPeriod.of(validFrom, validUntil))
            .minOrderAmount(Money.of(minOrderAmount))
            .build();
    }

    // 정률 할인 쿠폰 생성
    public static Coupon createPercentage(String name, Double discountRate, int totalQuantity,
        LocalDate validFrom, LocalDate validUntil, Long minOrderAmount) {
        return Coupon.builder()
            .name(name)
            .discountValue(DiscountValue.percentage(discountRate))
            .quantity(CouponQuantity.of(totalQuantity))
            .validPeriod(ValidPeriod.of(validFrom, validUntil))
            .minOrderAmount(Money.of(minOrderAmount))
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
        this.updatedAt = LocalDateTime.now();
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
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = CouponStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = CouponStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

}
