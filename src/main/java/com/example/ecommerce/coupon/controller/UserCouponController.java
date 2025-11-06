package com.example.ecommerce.coupon.controller;

import com.example.ecommerce.coupon.service.UserCouponService;
import com.example.ecommerce.coupon.service.UserCouponService.IssueCouponInput;
import com.example.ecommerce.coupon.service.UserCouponService.UserCouponOutput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/user-coupons")
@RequiredArgsConstructor
public class UserCouponController {

    private final UserCouponService userCouponService;

    /**
     * 쿠폰 발급
     */
    @PostMapping
    public ResponseEntity<UserCouponOutput> issueCoupon(
        @RequestBody @Valid IssueCouponInput input
    ) {
        UserCouponOutput output = userCouponService.issueCoupon(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    /**
     * 사용자의 쿠폰 목록 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<UserCouponOutput>> getUserCoupons(
        @PathVariable @NotNull @Positive Long userId
    ) {
        List<UserCouponOutput> userCoupons = userCouponService.getUserCoupons(userId);
        return ResponseEntity.ok(userCoupons);
    }

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     */
    @GetMapping("/users/{userId}/available")
    public ResponseEntity<List<UserCouponOutput>> getAvailableUserCoupons(
        @PathVariable @NotNull @Positive Long userId
    ) {
        List<UserCouponOutput> userCoupons = userCouponService.getAvailableUserCoupons(userId);
        return ResponseEntity.ok(userCoupons);
    }

    /**
     * 사용자 쿠폰 상세 조회
     */
    @GetMapping("/{userCouponId}")
    public ResponseEntity<UserCouponOutput> getUserCoupon(
        @PathVariable @NotNull @Positive Long userCouponId
    ) {
        UserCouponOutput userCoupon = userCouponService.getUserCoupon(userCouponId);
        return ResponseEntity.ok(userCoupon);
    }

    /**
     * 쿠폰 사용
     */
    @PostMapping("/{userCouponId}/use")
    public ResponseEntity<UserCouponOutput> useCoupon(
        @PathVariable @NotNull @Positive Long userCouponId
    ) {
        UserCouponOutput userCoupon = userCouponService.useCoupon(userCouponId);
        return ResponseEntity.ok(userCoupon);
    }
}
