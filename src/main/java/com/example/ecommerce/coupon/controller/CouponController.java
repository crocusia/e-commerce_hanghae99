package com.example.ecommerce.coupon.controller;

import com.example.ecommerce.common.dto.PageResponse;
import com.example.ecommerce.coupon.dto.CouponResponse;
import com.example.ecommerce.coupon.service.CouponService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final CouponService couponService;

    @Override
    public ResponseEntity<PageResponse<CouponResponse>> getAvailableCoupons(
        @PageableDefault(size = 20, sort = "validUntil", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        PageResponse<CouponResponse> response = PageResponse.empty(
            pageable.getPageNumber(),
            pageable.getPageSize()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CouponResponse> getCoupon(
        @PathVariable @Positive Long couponId
    ) {
        CouponResponse result = couponService.getCoupon(couponId);
        return ResponseEntity.ok(result);
    }
}
