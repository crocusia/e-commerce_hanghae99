package com.example.ecommerce.controller;

import com.example.ecommerce.api.UserCouponApi;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.service.CouponService;
import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.dto.coupon.IssueCouponRequest;
import com.example.ecommerce.dto.coupon.UserCouponResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/user-coupons")
@RequiredArgsConstructor
public class UserCouponController implements UserCouponApi {

    private final CouponService couponService;

    @Override
    public ResponseEntity<UserCouponResponse> issueCoupon(
        @RequestBody @Valid IssueCouponRequest request
    ) {
        UserCoupon userCoupon = couponService.issueCoupon(request.userId(), request.couponId());
        return ResponseEntity.status(201).body(convertToUserCouponResponse(userCoupon));
    }

    @Override
    public ResponseEntity<PageResponse<UserCouponResponse>> getUserCoupons(
        @PathVariable @Positive Long userId
        //@PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        List<UserCoupon> userCoupons = couponService.getUserCoupons(userId);
        List<UserCouponResponse> responses = userCoupons.stream()
            .map(this::convertToUserCouponResponse)
            .collect(Collectors.toList());

        PageResponse<UserCouponResponse> response = PageResponse.of(
            responses,
            0,  // pageable.getPageNumber()
            20,  // pageable.getPageSize()
            userCoupons.size()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserCouponResponse> getUserCoupon(
        @PathVariable @Positive Long userId,
        @PathVariable @Positive Long userCouponId
    ) {
        UserCoupon userCoupon = couponService.getUserCoupon(userCouponId);
        return ResponseEntity.ok(convertToUserCouponResponse(userCoupon));
    }

    private UserCouponResponse convertToUserCouponResponse(UserCoupon userCoupon) {
        return new UserCouponResponse(
            userCoupon.getId(),
            userCoupon.getUserId(),
            userCoupon.getCoupon().getId(),
            userCoupon.getCoupon().getName(),
            userCoupon.getCoupon().getDiscountPrice() != null ?
                userCoupon.getCoupon().getDiscountPrice().getAmount() : null,
            userCoupon.getCoupon().getDiscountRate(),
            userCoupon.getCoupon().getMinOrderAmount().getAmount(),
            userCoupon.getCoupon().getValidFrom(),
            userCoupon.getCoupon().getValidUntil(),
            userCoupon.getCreatedAt(),
            userCoupon.getUsedAt(),
            null,  // orderId - will be set when payment integration is complete
            convertToUserCouponStatus(userCoupon.getStatus())
        );
    }

    private com.example.ecommerce.dto.coupon.UserCouponStatus convertToUserCouponStatus(
        com.example.ecommerce.coupon.domain.UserCouponStatus status
    ) {
        return com.example.ecommerce.dto.coupon.UserCouponStatus.valueOf(status.name());
    }
}
