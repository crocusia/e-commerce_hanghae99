package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.coupon.domain.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(Long id);
    List<Coupon> findAll();
    List<Long> findExpiredCouponIds();

    default Coupon findByIdOrElseThrow(Long id) {
        return findById(id).orElseThrow(
            () -> new com.example.ecommerce.common.exception.CustomException(
                com.example.ecommerce.common.exception.ErrorCode.COUPON_NOT_FOUND
            )
        );
    }
}
