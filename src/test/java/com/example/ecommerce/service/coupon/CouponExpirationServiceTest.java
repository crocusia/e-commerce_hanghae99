package com.example.ecommerce.service.coupon;

import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.domain.UserCouponStatus;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.InMemoryCouponRepository;
import com.example.ecommerce.coupon.repository.InMemoryUserCouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.coupon.service.CouponExpirationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CouponExpirationService 테스트")
class CouponExpirationServiceTest {

    private CouponExpirationService couponExpirationService;
    private CouponRepository couponRepository;
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        couponRepository = new InMemoryCouponRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        couponExpirationService = new CouponExpirationService(couponRepository, userCouponRepository);

        ((InMemoryCouponRepository) couponRepository).clear();
        ((InMemoryUserCouponRepository) userCouponRepository).clear();
    }

    @Test
    @DisplayName("만료된 쿠폰들을 일괄 처리할 수 있다")
    void expireExpiredCoupons() {
        // given
        Coupon expiredCoupon = Coupon.createFixed(
            "만료된 쿠폰",
            5000L,
            100,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(1),
            10000L
        );
        Coupon validCoupon = Coupon.createFixed(
            "유효한 쿠폰",
            3000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            5000L
        );

        Coupon savedExpiredCoupon = couponRepository.save(expiredCoupon);
        Coupon savedValidCoupon = couponRepository.save(validCoupon);

        UserCoupon expiredUserCoupon1 = UserCoupon.create(1L, savedExpiredCoupon);
        UserCoupon expiredUserCoupon2 = UserCoupon.create(2L, savedExpiredCoupon);
        UserCoupon validUserCoupon = UserCoupon.create(3L, savedValidCoupon);

        userCouponRepository.save(expiredUserCoupon1);
        userCouponRepository.save(expiredUserCoupon2);
        userCouponRepository.save(validUserCoupon);

        // when
        int expiredCount = couponExpirationService.expireExpiredCoupons();

        // then
        assertThat(expiredCount).isEqualTo(2);

        List<UserCoupon> allCoupons = userCouponRepository.findByUserId(1L);
        allCoupons.addAll(userCouponRepository.findByUserId(2L));
        allCoupons.addAll(userCouponRepository.findByUserId(3L));

        long expiredStatusCount = allCoupons.stream()
            .filter(uc -> uc.getStatus() == UserCouponStatus.EXPIRED)
            .count();
        assertThat(expiredStatusCount).isEqualTo(2);

        long unusedStatusCount = allCoupons.stream()
            .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
            .count();
        assertThat(unusedStatusCount).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 사용된 쿠폰은 만료 처리하지 않는다")
    void expireExpiredCoupons_SkipUsedCoupons() {
        // given
        // 사용된 쿠폰을 만들기 위해 유효한 쿠폰으로 먼저 생성 후 사용 처리
        Coupon validCoupon = Coupon.createFixed(
            "유효한 쿠폰",
            5000L,
            100,
            LocalDate.now().minusDays(10),
            LocalDate.now().plusDays(20),
            10000L
        );
        Coupon savedValidCoupon = couponRepository.save(validCoupon);

        UserCoupon usedUserCoupon = UserCoupon.create(1L, savedValidCoupon);
        usedUserCoupon.use(); // 쿠폰 사용
        userCouponRepository.save(usedUserCoupon);

        // 이제 만료된 쿠폰 생성
        Coupon expiredCoupon = Coupon.createFixed(
            "만료된 쿠폰",
            3000L,
            100,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(1),
            5000L
        );
        Coupon savedExpiredCoupon = couponRepository.save(expiredCoupon);

        UserCoupon unusedUserCoupon = UserCoupon.create(2L, savedExpiredCoupon);
        userCouponRepository.save(unusedUserCoupon);

        // when
        int expiredCount = couponExpirationService.expireExpiredCoupons();

        // then
        assertThat(expiredCount).isEqualTo(1); // 사용되지 않은 만료 쿠폰만 처리

        UserCoupon savedUsedCoupon = userCouponRepository.findByUserIdAndCouponId(1L, savedValidCoupon.getId()).get();
        assertThat(savedUsedCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);

        UserCoupon savedUnusedCoupon = userCouponRepository.findByUserIdAndCouponId(2L, savedExpiredCoupon.getId()).get();
        assertThat(savedUnusedCoupon.getStatus()).isEqualTo(UserCouponStatus.EXPIRED);
    }

    @Test
    @DisplayName("만료 가능한 쿠폰이 없으면 0을 반환한다")
    void expireExpiredCoupons_NoCouponsToExpire() {
        // given
        Coupon validCoupon = Coupon.createFixed(
            "유효한 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedValidCoupon = couponRepository.save(validCoupon);

        UserCoupon validUserCoupon = UserCoupon.create(1L, savedValidCoupon);
        userCouponRepository.save(validUserCoupon);

        // when
        int expiredCount = couponExpirationService.expireExpiredCoupons();

        // then
        assertThat(expiredCount).isEqualTo(0);
    }


    @Test
    @DisplayName("만료 처리 결과를 조회할 수 있다")
    void getExpirationResult() {
        // given
        Coupon expiredCoupon = Coupon.createFixed(
            "만료된 쿠폰",
            5000L,
            100,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(1),
            10000L
        );
        Coupon savedExpiredCoupon = couponRepository.save(expiredCoupon);

        UserCoupon userCoupon1 = UserCoupon.create(1L, savedExpiredCoupon);
        UserCoupon userCoupon2 = UserCoupon.create(2L, savedExpiredCoupon);
        UserCoupon userCoupon3 = UserCoupon.create(3L, savedExpiredCoupon);

        userCouponRepository.save(userCoupon1);
        userCouponRepository.save(userCoupon2);
        userCouponRepository.save(userCoupon3);

        // when
        int totalExpired = couponExpirationService.expireExpiredCoupons();

        // then
        assertThat(totalExpired).isEqualTo(3);

        // 검증: 실제로 만료된 쿠폰 개수 확인
        List<UserCoupon> allUserCoupons = userCouponRepository.findByUserId(1L);
        allUserCoupons.addAll(userCouponRepository.findByUserId(2L));
        allUserCoupons.addAll(userCouponRepository.findByUserId(3L));

        long actualExpiredCount = allUserCoupons.stream()
            .filter(uc -> uc.getStatus() == UserCouponStatus.EXPIRED)
            .count();

        assertThat(actualExpiredCount).isEqualTo(totalExpired);
    }

    @Test
    @DisplayName("이미 만료 상태인 쿠폰은 중복 처리하지 않는다")
    void expireExpiredCoupons_SkipAlreadyExpired() {
        // given
        Coupon expiredCoupon = Coupon.createFixed(
            "만료된 쿠폰",
            5000L,
            100,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(1),
            10000L
        );
        Coupon savedExpiredCoupon = couponRepository.save(expiredCoupon);

        UserCoupon alreadyExpiredCoupon = UserCoupon.create(1L, savedExpiredCoupon);
        alreadyExpiredCoupon.expire(); // 이미 만료 처리
        UserCoupon notYetExpiredCoupon = UserCoupon.create(2L, savedExpiredCoupon);

        userCouponRepository.save(alreadyExpiredCoupon);
        userCouponRepository.save(notYetExpiredCoupon);

        // when
        int expiredCount = couponExpirationService.expireExpiredCoupons();

        // then
        assertThat(expiredCount).isEqualTo(1); // 아직 만료되지 않은 쿠폰만 처리
    }
}
