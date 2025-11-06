package com.example.ecommerce.coupon.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.domain.UserCouponStatus;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    private final Map<Long, ReentrantLock> couponLocks = new ConcurrentHashMap<>();

    @Schema(description = "쿠폰 발급 요청")
    public record IssueCouponInput(
        @NotNull(message = "사용자 ID는 필수입니다")
        @Positive(message = "사용자 ID는 양수여야 합니다")
        @Schema(description = "사용자 ID")
        Long userId,

        @NotNull(message = "쿠폰 ID는 필수입니다")
        @Positive(message = "쿠폰 ID는 양수여야 합니다")
        @Schema(description = "쿠폰 ID")
        Long couponId
    ) {}

    @Schema(description = "사용자 쿠폰 응답")
    public record UserCouponOutput(
        @Schema(description = "사용자 쿠폰 ID") Long id,
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "쿠폰 ID") Long couponId,
        @Schema(description = "쿠폰 이름") String couponName,
        @Schema(description = "할인 금액") Long discountPrice,
        @Schema(description = "할인율") Double discountRate,
        @Schema(description = "최소 주문 금액") Long minOrderAmount,
        @Schema(description = "유효 기간 시작") LocalDate validFrom,
        @Schema(description = "유효 기간 종료") LocalDate validUntil,
        @Schema(description = "발급 일시") LocalDateTime issuedAt,
        @Schema(description = "사용 일시") LocalDateTime usedAt,
        @Schema(description = "상태") UserCouponStatus status
    ) {
        public static UserCouponOutput from(UserCoupon userCoupon) {
            Coupon coupon = userCoupon.getCoupon();
            return new UserCouponOutput(
                userCoupon.getId(),
                userCoupon.getUserId(),
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountPrice() != null ? coupon.getDiscountPrice().getAmount() : null,
                coupon.getDiscountRate(),
                coupon.getMinOrderAmount().getAmount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                userCoupon.getCreatedAt(),
                userCoupon.getUsedAt(),
                userCoupon.getStatus()
            );
        }
    }

    /**
     * 선착순 쿠폰 발급 (1인 1매, 동시성 제어)
     */
    public UserCouponOutput issueCoupon(IssueCouponInput input) {
        Long userId = input.userId();
        Long couponId = input.couponId();

        // 1. 락 획득
        ReentrantLock lock = couponLocks.computeIfAbsent(couponId, k -> new ReentrantLock());
        lock.lock();

        try {
            // 2. 쿠폰 조회
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
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

            return UserCouponOutput.from(savedUserCoupon);

        } finally {
            // 6. 락 해제 (항상 실행)
            lock.unlock();
        }
    }

    /**
     * 사용자의 쿠폰 목록 조회
     */
    public List<UserCouponOutput> getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        return userCoupons.stream()
            .map(UserCouponOutput::from)
            .collect(Collectors.toList());
    }

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     */
    public List<UserCouponOutput> getAvailableUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        return userCoupons.stream()
            .filter(UserCoupon::canUse)
            .map(UserCouponOutput::from)
            .collect(Collectors.toList());
    }

    /**
     * 사용자 쿠폰 상세 조회
     */
    public UserCouponOutput getUserCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);
        return UserCouponOutput.from(userCoupon);
    }

    /**
     * 쿠폰 사용 (결제 시)
     */
    public UserCouponOutput useCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);
        userCoupon.use();
        UserCoupon usedCoupon = userCouponRepository.save(userCoupon);
        return UserCouponOutput.from(usedCoupon);
    }
}
