package com.example.ecommerce.coupon.controller;

import com.example.ecommerce.common.dto.PageResponse;
import com.example.ecommerce.coupon.dto.IssueCouponRequest;
import com.example.ecommerce.coupon.dto.UserCouponResponse;
import com.example.ecommerce.coupon.facade.CouponIssueFacade;
import com.example.ecommerce.coupon.service.CouponIssueQueryService;
import com.example.ecommerce.coupon.service.UserCouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/user-coupons")
@RequiredArgsConstructor
public class UserCouponController implements UserCouponApi {

    private final UserCouponService userCouponService;
    private final CouponIssueFacade couponIssueFacade; // 비동기 발급용
    private final CouponIssueQueryService couponIssueQueryService; // 상태 조회용

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

    @PostMapping("/issue-async")
    public ResponseEntity<AsyncIssueResponse> issueAsync(
        @RequestBody @Valid IssueCouponRequest request
    ) {
        log.info("쿠폰 비동기 발급 요청 - couponId: {}, userId: {}", request.couponId(), request.userId());

        couponIssueFacade.issueRequest(request.couponId(), request.userId());

        return ResponseEntity.ok(new AsyncIssueResponse(
            true,
            "쿠폰 발급 요청이 접수되었습니다. 잠시 후 발급 상태를 확인해주세요."
        ));
    }

    @GetMapping("/issue-status")
    public ResponseEntity<CouponIssueQueryService.CouponIssueStatusResponse> getIssueStatus(
        @RequestParam @Positive Long couponId,
        @RequestParam @Positive Long userId
    ) {
        log.info("쿠폰 발급 상태 조회 - couponId: {}, userId: {}", couponId, userId);

        CouponIssueQueryService.CouponIssueStatusResponse status =
            couponIssueQueryService.getIssueStatus(couponId, userId);

        return ResponseEntity.ok(status);
    }

    public record AsyncIssueResponse(
        boolean success,
        String message
    ) {
    }
}
