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
    private DiscountType discountType;
    private Money discountPrice;    // 정액 할인 금액
    private Double discountRate;    // 정률 할인율
    private int totalQuantity;      // 총 발급 수량
    private int issuedQuantity;     // 현재 발급된 수량
    private LocalDate validFrom;    // 유효 시작일
    private LocalDate validUntil;   // 유효 종료일
    private Money minOrderAmount;   // 최소 주문 금액
    private CouponStatus status;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Coupon(String name, DiscountType discountType, Money discountPrice, Double discountRate,
                   int totalQuantity, LocalDate validFrom, LocalDate validUntil, Money minOrderAmount) {
        validateName(name);
        validateQuantity(totalQuantity);
        validateValidPeriod(validFrom, validUntil);
        validateDiscount(discountType, discountPrice, discountRate);

        this.name = name;
        this.discountType = discountType;
        this.discountPrice = discountPrice;
        this.discountRate = discountRate;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
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
            DiscountType.FIXED,
            Money.of(discountPrice),
            null,
            totalQuantity,
            validFrom,
            validUntil,
            Money.of(minOrderAmount)
        );
    }

    // 정률 할인 쿠폰 생성
    public static Coupon createPercentage(String name, Double discountRate, int totalQuantity,
                                         LocalDate validFrom, LocalDate validUntil, Long minOrderAmount) {
        return new Coupon(
            name,
            DiscountType.PERCENTAGE,
            null,
            discountRate,
            totalQuantity,
            validFrom,
            validUntil,
            Money.of(minOrderAmount)
        );
    }

    // 쿠폰 발급
    public synchronized void issue() {
        if (!canIssue()) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (!isValid()) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }

        if (status != CouponStatus.ACTIVE) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        this.issuedQuantity++;
        this.updatedAt = LocalDateTime.now();
    }

    // 발급 가능 여부 확인
    public boolean canIssue() {
        return this.issuedQuantity < this.totalQuantity;
    }

    // 유효기간 확인
    public boolean isValid() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(validFrom) && !today.isAfter(validUntil);
    }

    // 할인 금액 계산
    public Money calculateDiscountAmount(Money orderAmount) {
        // 최소 주문 금액 확인
        if (orderAmount.isLessThan(minOrderAmount)) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (discountType == DiscountType.FIXED) {
            return discountPrice;
        } else {
            // 정률 할인: 주문 금액 * 할인율
            long discountAmount = (long) (orderAmount.getAmount() * discountRate / 100.0);
            return Money.of(discountAmount);
        }
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

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateValidPeriod(LocalDate validFrom, LocalDate validUntil) {
        if (validFrom == null || validUntil == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (validFrom.isAfter(validUntil)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateDiscount(DiscountType type, Money discountPrice, Double discountRate) {
        if (type == DiscountType.FIXED) {
            if (discountPrice == null || discountPrice.isLessThanOrEqual(Money.of(0L))) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (type == DiscountType.PERCENTAGE) {
            if (discountRate == null || discountRate <= 0 || discountRate > 100) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }
}
