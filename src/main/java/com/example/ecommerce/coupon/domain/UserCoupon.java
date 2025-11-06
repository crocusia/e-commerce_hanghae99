package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    private Long id;
    private Long userId;
    private Coupon coupon;
    private UserCouponStatus status;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserCoupon(Long userId, Coupon coupon) {
        validateUserId(userId);
        validateCoupon(coupon);

        this.userId = userId;
        this.coupon = coupon;
        this.status = UserCouponStatus.UNUSED;
        this.usedAt = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static UserCoupon create(Long userId, Coupon coupon) {
        return new UserCoupon(userId, coupon);
    }

    // 쿠폰 사용
    public void use() {
        if (status == UserCouponStatus.USED) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED);
        }

        if (status == UserCouponStatus.EXPIRED) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }

        if (!coupon.isValid()) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 사용 가능 여부 확인
    public boolean canUse() {
        return status == UserCouponStatus.UNUSED
            && coupon.isValid()
            && coupon.getStatus() == CouponStatus.ACTIVE;
    }

    // 사용 여부 확인
    public boolean isUsed() {
        return status == UserCouponStatus.USED;
    }

    // 만료 처리
    public void expire() {
        if (status == UserCouponStatus.USED) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.status = UserCouponStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    // 만료 여부 확인
    public boolean isExpired() {
        return status == UserCouponStatus.EXPIRED || !coupon.isValid();
    }

    // 검증 메서드
    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateCoupon(Coupon coupon) {
        if (coupon == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
