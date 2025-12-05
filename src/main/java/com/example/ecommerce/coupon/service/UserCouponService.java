package com.example.ecommerce.coupon.service;

import com.example.ecommerce.common.aop.DistributedLock;
import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.dto.UserCouponResponse;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @DistributedLock(key = "'coupon:lock:' + #couponId", waitTime = 5, leaseTime = 3)
    @Transactional
    public UserCouponResponse issueCoupon(long couponId, Long userId) {
        log.info("쿠폰 발급 시작 - couponId: {}, userId: {}", couponId, userId);

        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);

        Optional<UserCoupon> checkCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId);
        if(checkCoupon.isPresent()){
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        coupon.issue();
        couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
            .userId(userId)
            .coupon(coupon)
            .expiresAt(coupon.getValidPeriod().getValidUntil())
            .build();

        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        log.info("쿠폰 발급 완료 - userCouponId: {}", savedUserCoupon.getId());

        return UserCouponResponse.from(savedUserCoupon, coupon);
    }

    @Transactional(readOnly = true)
    public List<UserCouponResponse> getAvailableUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        return userCoupons.stream()
            .filter(UserCoupon::canUse)
            .map(userCoupon -> {
                Coupon coupon = couponRepository.findByIdOrElseThrow(userCoupon.getCoupon().getId());
                return UserCouponResponse.from(userCoupon, coupon);
            })
            .collect(Collectors.toList());
    }


    @Transactional
    public void reserveCoupon(Long userId, Long userCouponId) {
        log.info("쿠폰 예약 시작 - userId: {}, userCouponId: {}", userId, userCouponId);

        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);

        // 소유자 검증
        if (!userCoupon.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "본인의 쿠폰만 사용할 수 있습니다.");
        }

        // 쿠폰 유효성 검증 및 예약
        userCoupon.reserve();
        userCouponRepository.save(userCoupon);

        log.info("쿠폰 예약 완료 - userCouponId: {}", userCouponId);
    }

    @Transactional
    public UserCouponResponse useCoupon(Long userCouponId) {

        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);

        userCoupon.use();
        UserCoupon usedCoupon = userCouponRepository.save(userCoupon);

        Coupon coupon = couponRepository.findByIdOrElseThrow(usedCoupon.getCoupon().getId());

        return UserCouponResponse.from(usedCoupon, coupon);
    }

    @Transactional(readOnly = true)
    public UserCouponResponse getUserCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);

        Coupon coupon = couponRepository.findByIdOrElseThrow(userCoupon.getCoupon().getId());

        return UserCouponResponse.from(userCoupon, coupon);
    }

    @Transactional
    public UserCoupon issueCouponAsync(Long couponId, Long userId) {
        log.info("쿠폰 비동기 발급 - couponId: {}, userId: {}", couponId, userId);

        // 중복 체크 (DB 레벨)
        Optional<UserCoupon> checkCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId);
        if (checkCoupon.isPresent()) {
            log.warn("이미 발급된 쿠폰 - couponId: {}, userId: {}", couponId, userId);
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);

        // 수량 증가
        coupon.issue();
        couponRepository.save(coupon);

        // UserCoupon 생성
        UserCoupon userCoupon = UserCoupon.create(userId, coupon);
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        log.info("쿠폰 비동기 발급 완료 - userCouponId: {}", savedUserCoupon.getId());

        return savedUserCoupon;
    }
}
