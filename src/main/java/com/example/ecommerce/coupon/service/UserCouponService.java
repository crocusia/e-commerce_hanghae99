package com.example.ecommerce.coupon.service;

import com.example.ecommerce.common.aop.PessimisticLock;
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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @PessimisticLock(key = "#couponId")
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
            .couponId(coupon.getId())
            .expiresAt(coupon.getValidPeriod().getValidUntil().atStartOfDay())
            .build();

        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        log.info("쿠폰 발급 완료 - userCouponId: {}", savedUserCoupon.getId());

        return UserCouponResponse.from(savedUserCoupon, coupon);
    }

    public List<UserCouponResponse> getAvailableUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        return userCoupons.stream()
            .filter(UserCoupon::canUse)
            .map(userCoupon -> {
                Coupon coupon = couponRepository.findByIdOrElseThrow(userCoupon.getCouponId());
                return UserCouponResponse.from(userCoupon, coupon);
            })
            .collect(Collectors.toList());
    }

    public UserCouponResponse useCoupon(Long userCouponId) {

        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);

        userCoupon.use();
        UserCoupon usedCoupon = userCouponRepository.save(userCoupon);

        Coupon coupon = couponRepository.findByIdOrElseThrow(usedCoupon.getCouponId());

        return UserCouponResponse.from(usedCoupon, coupon);
    }

    public UserCouponResponse getUserCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);

        Coupon coupon = couponRepository.findByIdOrElseThrow(userCoupon.getCouponId());

        return UserCouponResponse.from(userCoupon, coupon);
    }
}
