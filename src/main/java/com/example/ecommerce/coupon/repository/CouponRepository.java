package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.coupon.domain.Coupon;

import com.example.ecommerce.coupon.domain.status.CouponStatus;
import java.util.List;
import java.util.Optional;
import javax.swing.ListModel;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    Coupon findByIdOrElseThrow(Long id);

    List<Coupon> findByStatus(CouponStatus status);
}
