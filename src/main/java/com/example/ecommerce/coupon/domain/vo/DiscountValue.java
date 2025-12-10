package com.example.ecommerce.coupon.domain.vo;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.status.DiscountType;
import com.example.ecommerce.product.domain.vo.Money;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountValue {
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;
    private Long discountPrice;
    private Double discountRate;

    private DiscountValue(DiscountType discountType, Long discountPrice, Double discountRate) {
        validateDiscount(discountType, discountPrice, discountRate);
        this.discountType = discountType;
        this.discountPrice = discountPrice;
        this.discountRate = discountRate;
    }

    public static DiscountValue fixed(Long discountPrice) {
        return new DiscountValue(DiscountType.FIXED, discountPrice, null);
    }

    public static DiscountValue percentage(Double discountRate) {
        return new DiscountValue(DiscountType.PERCENTAGE, null, discountRate);
    }

    public Money calculateDiscountAmount(Money orderAmount) {
        if (discountType == DiscountType.FIXED) {
            return Money.of(discountPrice);
        } else {
            long discountAmount = (long) (orderAmount.getAmount() * discountRate / 100.0);
            return Money.of(discountAmount);
        }
    }

    public boolean isFixed() {
        return discountType == DiscountType.FIXED;
    }

    public boolean isPercentage() {
        return discountType == DiscountType.PERCENTAGE;
    }

    private static void validateDiscount(DiscountType type, Long discountPrice, Double discountRate) {
        if (type == DiscountType.FIXED) {
            if (discountPrice == null || discountPrice <= 0) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (type == DiscountType.PERCENTAGE) {
            if (discountRate == null || discountRate <= 0 || discountRate > 100) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscountValue that = (DiscountValue) o;
        return discountType == that.discountType &&
            Objects.equals(discountPrice, that.discountPrice) &&
            Objects.equals(discountRate, that.discountRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discountType, discountPrice, discountRate);
    }

    @Override
    public String toString() {
        if (discountType == DiscountType.FIXED) {
            return "DiscountValue{type=FIXED, price=" + discountPrice + "}";
        } else {
            return "DiscountValue{type=PERCENTAGE, rate=" + discountRate + "%}";
        }
    }
}
