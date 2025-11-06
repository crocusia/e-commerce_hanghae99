package com.example.ecommerce.coupon.service;

import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponExpirationService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 쿠폰 만료 처리 - 추후 배치 기반 처리 가능, 필터링을 위한 Coupon 상태 추가 가능
     */
    public int expireExpiredCoupons() {
        log.info("Starting expiration process for all expired coupons");

        // 1단계: 유효기간이 지난 쿠폰 ID 조회 (인덱스 활용 가능)
        List<Long> expiredCouponIds = couponRepository.findExpiredCouponIds();

        if (expiredCouponIds.isEmpty()) {
            log.info("No expired coupons found");
            return 0;
        }

        log.info("Found {} expired coupon types", expiredCouponIds.size());

        // 2단계: 해당 쿠폰 ID를 가진 UNUSED 상태의 UserCoupon만 조회
        // (couponId + status 복합 인덱스 활용 가능)
        List<UserCoupon> unusedExpiredCoupons =
            userCouponRepository.findByCouponIdsAndUnusedStatus(expiredCouponIds);

        // 3단계: 만료 처리
        unusedExpiredCoupons.forEach(UserCoupon::expire);
        unusedExpiredCoupons.forEach(userCouponRepository::save);

        int expiredCount = unusedExpiredCoupons.size();
        log.info("Expired {} user coupons", expiredCount);

        return expiredCount;
    }

}
