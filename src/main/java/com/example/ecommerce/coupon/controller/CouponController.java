package com.example.ecommerce.coupon.controller;

import com.example.ecommerce.coupon.service.CouponService;
import com.example.ecommerce.coupon.service.CouponService.CouponDetailOutput;
import com.example.ecommerce.coupon.service.CouponService.CouponOutput;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 발급 가능한 쿠폰 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<CouponOutput>> getAvailableCoupons() {
        List<CouponOutput> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(coupons);
    }

    /**
     * 쿠폰 상세 조회
     */
    @GetMapping("/{couponId}")
    public ResponseEntity<CouponDetailOutput> getCoupon(
        @PathVariable @NotNull @Positive Long couponId
    ) {
        CouponDetailOutput coupon = couponService.getCoupon(couponId);
        return ResponseEntity.ok(coupon);
    }
}
