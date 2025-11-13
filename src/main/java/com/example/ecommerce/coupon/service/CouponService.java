package com.example.ecommerce.coupon.service;

import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.status.CouponStatus;
import com.example.ecommerce.coupon.dto.CouponResponse;
import com.example.ecommerce.coupon.repository.CouponRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public List<CouponResponse> getAllCouponsAvailable() {
        List<Coupon> coupons = couponRepository.findByStatus(CouponStatus.ACTIVE);
        return coupons.stream()
            .map(CouponResponse::from)
            .collect(Collectors.toList());
    }

    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);
        return CouponResponse.from(coupon);
    }
}
