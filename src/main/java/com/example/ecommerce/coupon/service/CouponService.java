package com.example.ecommerce.coupon.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 선착순 쿠폰 발급 (1인 1매, 동시성 제어)
     */
    public synchronized UserCoupon issueCoupon(Long userId, Long couponId) {
        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);

        // 2. 중복 발급 체크 (1인 1매)
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED);
        }

        // 3. 쿠폰 발급 (수량 체크, 유효기간 체크, 상태 체크 포함)
        coupon.issue();
        couponRepository.save(coupon);

        // 4. 사용자 쿠폰 생성
        UserCoupon userCoupon = UserCoupon.create(userId, coupon);
        return userCouponRepository.save(userCoupon);
    }

    /**
     * 사용자의 쿠폰 목록 조회
     */
    public List<UserCoupon> getUserCoupons(Long userId) {
        return userCouponRepository.findByUserId(userId);
    }

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     */
    public List<UserCoupon> getAvailableUserCoupons(Long userId) {
        return userCouponRepository.findByUserId(userId).stream()
            .filter(UserCoupon::canUse)
            .collect(Collectors.toList());
    }

    /**
     * 쿠폰 상세 조회
     */
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findByIdOrElseThrow(couponId);
    }

    /**
     * 전체 쿠폰 목록 조회
     */
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    /**
     * 쿠폰 사용 (결제 시)
     */
    public UserCoupon useCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);
        userCoupon.use();
        return userCouponRepository.save(userCoupon);
    }

    /**
     * 사용자 쿠폰 조회
     */
    public UserCoupon getUserCoupon(Long userCouponId) {
        return userCouponRepository.findByIdOrElseThrow(userCouponId);
    }
}
