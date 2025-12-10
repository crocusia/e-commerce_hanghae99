package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.coupon.domain.UserCoupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {

    Optional<UserCoupon> findById(Long id);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    UserCoupon findByIdOrElseThrow(Long id);

    List<UserCoupon> findByUserId(Long userId);

    UserCoupon save(UserCoupon userCoupon);

    void deleteAllInBatch();
}
