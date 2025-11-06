package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.Money;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    private Long id;
    private String name;
    private DiscountValue discountValue;    // 할인 값 (타입 + 금액/비율)
    private CouponQuantity quantity;        // 쿠폰 수량 (총 수량 + 발급 수량)
    private ValidPeriod validPeriod;        // 유효 기간
    private Money minOrderAmount;           // 최소 주문 금액
    private CouponStatus status;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Coupon(String name, DiscountValue discountValue, CouponQuantity quantity,
                   ValidPeriod validPeriod, Money minOrderAmount) {
        validateName(name);

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
        return new Coupon(
            name,
            DiscountValue.fixed(Money.of(discountPrice)),
            CouponQuantity.of(totalQuantity),
            ValidPeriod.of(validFrom, validUntil),
            Money.of(minOrderAmount)
        );
    }

    // 정률 할인 쿠폰 생성
    public static Coupon createPercentage(String name, Double discountRate, int totalQuantity,
                                         LocalDate validFrom, LocalDate validUntil, Long minOrderAmount) {
        return new Coupon(
            name,
            DiscountValue.percentage(discountRate),
            CouponQuantity.of(totalQuantity),
            ValidPeriod.of(validFrom, validUntil),
            Money.of(minOrderAmount)
        );
    }

    // 쿠폰 발급
    public void issue() {
        if (!canIssue()) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (!isValid()) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }

        if (status != CouponStatus.ACTIVE) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        this.quantity = this.quantity.issue();
        this.updatedAt = LocalDateTime.now();
    }

    // 발급 가능 여부 확인
    public boolean canIssue() {
        return this.quantity.canIssue();
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

    // 검증 메서드
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    // VO를 통한 접근 메서드
    public DiscountType getDiscountType() {
        return discountValue.getDiscountType();
    }

    public Money getDiscountPrice() {
        return discountValue.getDiscountPrice();
    }

    public Double getDiscountRate() {
        return discountValue.getDiscountRate();
    }

    public int getTotalQuantity() {
        return quantity.getTotalQuantity();
    }

    public int getIssuedQuantity() {
        return quantity.getIssuedQuantity();
    }

    public LocalDate getValidFrom() {
        return validPeriod.getValidFrom();
    }

    public LocalDate getValidUntil() {
        return validPeriod.getValidUntil();
    }
}
