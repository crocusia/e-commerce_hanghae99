package com.example.ecommerce.coupon.service;

import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.CouponStatus;
import com.example.ecommerce.coupon.domain.DiscountType;
import com.example.ecommerce.coupon.repository.CouponRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Schema(description = "쿠폰 응답")
    public record CouponOutput(
        @Schema(description = "쿠폰 ID") Long id,
        @Schema(description = "쿠폰 이름") String name,
        @Schema(description = "할인 타입") DiscountType discountType,
        @Schema(description = "할인 금액") Long discountPrice,
        @Schema(description = "할인율") Double discountRate,
        @Schema(description = "총 수량") Integer totalQuantity,
        @Schema(description = "발급된 수량") Integer issuedQuantity,
        @Schema(description = "남은 수량") Integer remainingQuantity,
        @Schema(description = "유효 기간 시작") LocalDate validFrom,
        @Schema(description = "유효 기간 종료") LocalDate validUntil,
        @Schema(description = "최소 주문 금액") Long minOrderAmount,
        @Schema(description = "쿠폰 상태") CouponStatus status
    ) {
        public static CouponOutput from(Coupon coupon) {
            return new CouponOutput(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountType(),
                coupon.getDiscountPrice() != null ? coupon.getDiscountPrice().getAmount() : null,
                coupon.getDiscountRate(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getTotalQuantity() - coupon.getIssuedQuantity(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getMinOrderAmount().getAmount(),
                coupon.getStatus()
            );
        }
    }

    @Schema(description = "쿠폰 상세 정보 응답")
    public record CouponDetailOutput(
        @Schema(description = "쿠폰 ID") Long id,
        @Schema(description = "쿠폰 이름") String name,
        @Schema(description = "할인 타입") DiscountType discountType,
        @Schema(description = "할인 금액") Long discountPrice,
        @Schema(description = "할인율") Double discountRate,
        @Schema(description = "총 수량") Integer totalQuantity,
        @Schema(description = "발급된 수량") Integer issuedQuantity,
        @Schema(description = "남은 수량") Integer remainingQuantity,
        @Schema(description = "유효 기간 시작") LocalDate validFrom,
        @Schema(description = "유효 기간 종료") LocalDate validUntil,
        @Schema(description = "최소 주문 금액") Long minOrderAmount,
        @Schema(description = "쿠폰 상태") CouponStatus status,
        @Schema(description = "발급 가능 여부") Boolean canIssue,
        @Schema(description = "유효 여부") Boolean isValid
    ) {
        public static CouponDetailOutput from(Coupon coupon) {
            return new CouponDetailOutput(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountType(),
                coupon.getDiscountPrice() != null ? coupon.getDiscountPrice().getAmount() : null,
                coupon.getDiscountRate(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getTotalQuantity() - coupon.getIssuedQuantity(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getMinOrderAmount().getAmount(),
                coupon.getStatus(),
                coupon.canIssue(),
                coupon.isValid()
            );
        }
    }

    /**
     * 발급 가능한 쿠폰 목록 조회
     */
    public List<CouponOutput> getAllCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        return coupons.stream()
            .map(CouponOutput::from)
            .collect(Collectors.toList());
    }

    /**
     * 쿠폰 상세 조회
     */
    public CouponDetailOutput getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);
        return CouponDetailOutput.from(coupon);
    }
}
