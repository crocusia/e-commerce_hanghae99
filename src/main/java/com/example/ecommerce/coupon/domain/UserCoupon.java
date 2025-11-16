package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.domain.BaseEntity;
import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.status.UserCouponStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserCouponStatus status;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static UserCoupon create(Long userId, Coupon coupon){
        return UserCoupon.builder()
            .userId(userId)
            .coupon(coupon)
            .status(UserCouponStatus.UNUSED)
            .expiresAt(coupon.getValidPeriod().getValidUntil())
            .build();
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
