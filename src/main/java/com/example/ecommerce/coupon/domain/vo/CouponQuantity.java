package com.example.ecommerce.coupon.domain.vo;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponQuantity {

    private int totalQuantity;      // 총 발급 수량
    private int issuedQuantity;     // 현재 발급된 수량

    private CouponQuantity(int totalQuantity, int issuedQuantity) {
        validateTotalQuantity(totalQuantity);
        validateIssuedQuantity(issuedQuantity, totalQuantity);
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
    }

    public static CouponQuantity of(int totalQuantity) {
        return new CouponQuantity(totalQuantity, 0);
    }

    public static CouponQuantity of(int totalQuantity, int issuedQuantity) {
        return new CouponQuantity(totalQuantity, issuedQuantity);
    }

    public CouponQuantity issue() {
        if (!canIssue()) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        return new CouponQuantity(this.totalQuantity, this.issuedQuantity + 1);
    }

    public boolean canIssue() {
        return this.issuedQuantity < this.totalQuantity;
    }

    public int getRemainingQuantity() {
        return this.totalQuantity - this.issuedQuantity;
    }

    private static void validateTotalQuantity(int totalQuantity) {
        if (totalQuantity <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateIssuedQuantity(int issuedQuantity, int totalQuantity) {
        if (issuedQuantity < 0 || issuedQuantity > totalQuantity) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CouponQuantity that = (CouponQuantity) o;
        return totalQuantity == that.totalQuantity && issuedQuantity == that.issuedQuantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalQuantity, issuedQuantity);
    }

    @Override
    public String toString() {
        return "CouponQuantity{issued=" + issuedQuantity + "/" + totalQuantity + "}";
    }
}