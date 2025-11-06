package com.example.ecommerce.controller;

import com.example.ecommerce.api.CouponApi;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.service.CouponService;
import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.dto.coupon.CouponResponse;
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
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final CouponService couponService;

    @Override
    public ResponseEntity<PageResponse<CouponResponse>> getAvailableCoupons(
        //@PageableDefault(size = 20, sort = "validUntil", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        List<Coupon> coupons = couponService.getAllCoupons();
        List<CouponResponse> couponResponses = coupons.stream()
            .map(this::convertToCouponResponse)
            .collect(Collectors.toList());

        PageResponse<CouponResponse> response = PageResponse.of(
            couponResponses,
            0,  // pageable.getPageNumber()
            20,  // pageable.getPageSize()
            coupons.size()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CouponResponse> getCoupon(
        @PathVariable @Positive Long couponId
    ) {
        Coupon coupon = couponService.getCoupon(couponId);
        return ResponseEntity.ok(convertToCouponResponse(coupon));
    }

    private CouponResponse convertToCouponResponse(Coupon coupon) {
        return new CouponResponse(
            coupon.getId(),
            coupon.getName(),
            coupon.getDiscountPrice() != null ? coupon.getDiscountPrice().getAmount() : null,
            coupon.getDiscountRate(),
            coupon.getTotalQuantity(),
            coupon.getIssuedQuantity(),
            coupon.getTotalQuantity() - coupon.getIssuedQuantity(),  // remainingQuantity
            coupon.getValidFrom(),
            coupon.getValidUntil(),
            coupon.getMinOrderAmount().getAmount().intValue(),
            convertToCouponStatus(coupon.getStatus())
        );
    }

    private com.example.ecommerce.dto.coupon.CouponStatus convertToCouponStatus(
        com.example.ecommerce.coupon.domain.CouponStatus status
    ) {
        return com.example.ecommerce.dto.coupon.CouponStatus.valueOf(status.name());
    }
}
