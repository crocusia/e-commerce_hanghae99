package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.coupon.domain.UserCoupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {
    UserCoupon save(UserCoupon userCoupon);
    Optional<UserCoupon> findById(Long id);
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
    List<UserCoupon> findByUserId(Long userId);
    List<UserCoupon> findByCouponId(Long couponId);
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
    void deleteById(Long id);

    default UserCoupon findByIdOrElseThrow(Long id) {
        return findById(id).orElseThrow(
            () -> new com.example.ecommerce.common.exception.CustomException(
                com.example.ecommerce.common.exception.ErrorCode.COUPON_NOT_FOUND
            )
        );
    }
}
