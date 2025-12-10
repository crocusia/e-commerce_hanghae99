package com.example.ecommerce.coupon.dto;

import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.status.CouponStatus;
import com.example.ecommerce.coupon.domain.status.DiscountType;
import com.example.ecommerce.coupon.domain.vo.DiscountValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "쿠폰 응답")
public record CouponResponse(
    @Schema(description = "쿠폰 ID") Long id,
    @Schema(description = "쿠폰 이름") String name,
    @Schema(description = "할인 타입") DiscountType discountType,
    @Schema(description = "할인 금액") Long discountPrice,
    @Schema(description = "할인율") Double discountRate,
    @Schema(description = "총 수량") Integer totalQuantity,
    @Schema(description = "발급된 수량") Integer issuedQuantity,
    @Schema(description = "남은 수량") Integer remainingQuantity,
    @Schema(description = "유효 기간 시작") LocalDateTime validFrom,
    @Schema(description = "유효 기간 종료") LocalDateTime validUntil,
    @Schema(description = "최소 주문 금액") Long minOrderAmount,
    @Schema(description = "발급 가능 여부") Boolean canIssue,
    @Schema(description = "쿠폰 상태") CouponStatus status
) {
    public static CouponResponse from(Coupon coupon) {
        DiscountValue discountValue = coupon.getDiscountValue();

        Long safeDiscountPrice = discountValue.getDiscountPrice() != null
            ? discountValue.getDiscountPrice()
            : null;

        Double safeDiscountRate = discountValue.getDiscountRate() != null
            ? discountValue.getDiscountRate()
            : null;

        return new CouponResponse(
            coupon.getId(),
            coupon.getName(),
            coupon.getDiscountValue().getDiscountType(),
            safeDiscountPrice,
            safeDiscountRate,
            coupon.getQuantity().getTotalQuantity(),
            coupon.getQuantity().getIssuedQuantity(),
            coupon.getQuantity().getTotalQuantity() - coupon.getQuantity().getIssuedQuantity(),
            coupon.getValidPeriod().getValidFrom(),
            coupon.getValidPeriod().getValidUntil(),
            coupon.getMinOrderAmount().getAmount(),
            coupon.canIssue(),
            coupon.getStatus()
        );
    }
}
