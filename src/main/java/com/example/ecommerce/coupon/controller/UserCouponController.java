package com.example.ecommerce.coupon.controller;

import com.example.ecommerce.common.dto.PageResponse;
import com.example.ecommerce.coupon.dto.IssueCouponRequest;
import com.example.ecommerce.coupon.dto.UserCouponResponse;
import com.example.ecommerce.coupon.service.UserCouponService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/user-coupons")
@RequiredArgsConstructor
public class UserCouponController implements UserCouponApi {

    private final UserCouponService userCouponService;

    @Override
    public ResponseEntity<UserCouponResponse> issueCoupon(
        @RequestBody @Valid IssueCouponRequest request
    ) {
        UserCouponResponse result = userCouponService.issueCoupon(request.userId(), request.couponId());
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<PageResponse<UserCouponResponse>> getUserCoupons(
        @PathVariable @Positive Long userId,
        @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
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
        UserCouponResponse result = userCouponService.getUserCoupon(userCouponId);
        return ResponseEntity.ok(result);
    }
}
