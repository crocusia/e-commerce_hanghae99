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

    @Column(name = "coupon_id")
    private Long couponId;

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

    @Version
    @Column(name = "version")
    private Long version;

    public static UserCoupon create(Long userId, Coupon coupon){
        return UserCoupon.builder()
            .userId(userId)
            .couponId(coupon.getId())
            .coupon(coupon)
            .status(UserCouponStatus.UNUSED)
            .expiresAt(coupon.getValidPeriod().getValidUntil())
            .build();
    }

    public void reserve() {
        if (status == UserCouponStatus.RESERVED) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED, "이미 예약된 쿠폰입니다.");
        }
        if (isUsed()) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED, "이미 사용된 쿠폰입니다.");
        }
        if (isExpired()) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }
        if (status != UserCouponStatus.UNUSED) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE, "사용할 수 없는 상태의 쿠폰입니다.");
        }
        this.status = UserCouponStatus.RESERVED;
    }

    public void cancelReservation() {
        if (status != UserCouponStatus.RESERVED) {
            throw new IllegalStateException("예약 상태의 쿠폰만 취소할 수 있습니다.");
        }
        this.status = UserCouponStatus.UNUSED;
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
        return (status == UserCouponStatus.UNUSED || status == UserCouponStatus.RESERVED) && !isExpired();
    }

    public boolean isUsed() {
        return status == UserCouponStatus.USED;
    }

    public boolean isExpired() {
        return status == UserCouponStatus.EXPIRED || LocalDateTime.now().isAfter(expiresAt);
    }

}
