package com.example.ecommerce.controller;

import com.example.ecommerce.api.CouponApi;
import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.dto.coupon.CouponResponse;
import jakarta.validation.constraints.Positive;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/coupons")
public class CouponController implements CouponApi {

    @Override
    public ResponseEntity<PageResponse<CouponResponse>> getAvailableCoupons(
        //@PageableDefault(size = 20, sort = "validUntil", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        PageResponse<CouponResponse> response = PageResponse.empty(
            0,  // pageable.getPageNumber()
            20  // pageable.getPageSize()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CouponResponse> getCoupon(
        @PathVariable @Positive Long couponId
    ) {
        CouponResponse mockResponse = new CouponResponse(
            couponId,
            "Mock",
            1L,
            0.5,
            0,
            0,
            0,
            null,
            null,
            0,
            null
        );

        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }
}
