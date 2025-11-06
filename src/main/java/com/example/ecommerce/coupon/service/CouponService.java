package com.example.ecommerce.coupon.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    private final Map<Long, ReentrantLock> couponLocks = new ConcurrentHashMap<>();
    /**
     * 선착순 쿠폰 발급 (1인 1매, 동시성 제어)
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        // 1. 락 획득
        ReentrantLock lock = couponLocks.computeIfAbsent(couponId, k -> new ReentrantLock());
        lock.lock();

        try {
            // 2. 쿠폰 조회 (순수 데이터 접근)
            Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

            // 3. 중복 발급 체크
            if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
                throw new CustomException(ErrorCode.COUPON_ALREADY_USED);
            }

            // 4. 쿠폰 발급
            coupon.issue();
            couponRepository.save(coupon);

            // 5. 사용자 쿠폰 생성
            UserCoupon userCoupon = UserCoupon.create(userId, coupon);
            return userCouponRepository.save(userCoupon);

        } finally {
            // 6. 락 해제 (항상 실행)
            lock.unlock();
        }
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
