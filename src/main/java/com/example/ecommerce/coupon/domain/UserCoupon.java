package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.status.UserCouponStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserCoupon {
    private final Long id;
    private final Long userId;
    private final Long couponId;
    private UserCouponStatus status;
    private LocalDateTime usedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    @Builder
    private UserCoupon(Long id, Long userId, Long couponId, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.couponId = couponId;
        this.status = UserCouponStatus.UNUSED;
        this.usedAt = null;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public void use() {
        if(isUsed()){
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED);
        }
        if(isExpired()){
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }
        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public boolean canUse() {
        return status == UserCouponStatus.UNUSED && !isExpired();
    }

    public boolean isUsed() {
        return status == UserCouponStatus.USED;
    }

    public boolean isExpired() {
        return status == UserCouponStatus.EXPIRED || LocalDateTime.now().isAfter(expiresAt);
    }

}
