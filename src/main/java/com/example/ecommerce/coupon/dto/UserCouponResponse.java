package com.example.ecommerce.coupon.dto;

import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.domain.status.UserCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 쿠폰 응답")
public record UserCouponResponse(
    @Schema(description = "사용자 쿠폰 ID") Long id,
    @Schema(description = "사용자 ID") Long userId,
    @Schema(description = "쿠폰 ID") Long couponId,
    @Schema(description = "쿠폰 이름") String couponName,
    @Schema(description = "할인 금액") Long discountPrice,
    @Schema(description = "할인율") Double discountRate,
    @Schema(description = "최소 주문 금액") Long minOrderAmount,
    @Schema(description = "유효 기간 시작") LocalDateTime validFrom,
    @Schema(description = "유효 기간 종료") LocalDateTime validUntil,
    @Schema(description = "발급 일시") LocalDateTime issuedAt,
    @Schema(description = "사용 일시") LocalDateTime usedAt,
    @Schema(description = "상태") UserCouponStatus status
) {
    public static UserCouponResponse from(UserCoupon userCoupon, Coupon coupon) {
        return new UserCouponResponse(
            userCoupon.getId(),
            userCoupon.getUserId(),
            coupon.getId(),
            coupon.getName(),
            coupon.getDiscountValue().getDiscountPrice(),
            coupon.getDiscountValue().getDiscountRate(),
            coupon.getMinOrderAmount().getAmount(),
            coupon.getValidPeriod().getValidFrom(),
            coupon.getValidPeriod().getValidUntil(),
            userCoupon.getCreatedAt(),
            userCoupon.getUsedAt(),
            userCoupon.getStatus()
        );
    }
}
