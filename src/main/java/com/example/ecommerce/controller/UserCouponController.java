package com.example.ecommerce.controller;

import com.example.ecommerce.api.UserCouponApi;
import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.dto.coupon.IssueCouponRequest;
import com.example.ecommerce.dto.coupon.UserCouponResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/user-coupons")
public class UserCouponController implements UserCouponApi {

    @Override
    public ResponseEntity<UserCouponResponse> issueCoupon(
        @RequestBody @Valid IssueCouponRequest request
    ) {
        UserCouponResponse mockResponse = new UserCouponResponse(
            null,
            request.userId(),
            request.couponId(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.status(201).body(mockResponse);
    }

    @Override
    public ResponseEntity<PageResponse<UserCouponResponse>> getUserCoupons(
        @PathVariable @Positive Long userId,
        @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        PageResponse<UserCouponResponse> response = PageResponse.empty(
            pageable.getPageNumber(),
            pageable.getPageSize()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserCouponResponse> getUserCoupon(
        @PathVariable @Positive Long userId,
        @PathVariable @Positive Long userCouponId
    ) {
        //Mock 데이터
        UserCouponResponse mockResponse = new UserCouponResponse(
            userCouponId,
            userId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }
}
