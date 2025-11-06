package com.example.ecommerce.service.coupon;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.InMemoryCouponRepository;
import com.example.ecommerce.coupon.repository.InMemoryUserCouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.coupon.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CouponService 테스트")
class CouponServiceTest {

    private CouponService couponService;
    private CouponRepository couponRepository;
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        couponRepository = new InMemoryCouponRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        couponService = new CouponService(couponRepository, userCouponRepository);

        ((InMemoryCouponRepository) couponRepository).clear();
        ((InMemoryUserCouponRepository) userCouponRepository).clear();
    }

    @Test
    @DisplayName("쿠폰을 발급받을 수 있다")
    void issueCoupon() {
        // given
        Coupon coupon = Coupon.createFixed(
            "5000원 할인 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);
        Long userId = 1L;

        // when
        UserCoupon userCoupon = couponService.issueCoupon(userId, savedCoupon.getId());

        // then
        assertThat(userCoupon).isNotNull();
        assertThat(userCoupon.getUserId()).isEqualTo(userId);
        assertThat(userCoupon.getCoupon().getId()).isEqualTo(savedCoupon.getId());
    }

    @Test
    @DisplayName("한 사용자는 같은 쿠폰을 한 번만 발급받을 수 있다 (1인 1매)")
    void issueCoupon_OnePerUser() {
        // given
        Coupon coupon = Coupon.createFixed(
            "5000원 할인 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);
        Long userId = 1L;

        // when
        couponService.issueCoupon(userId, savedCoupon.getId());

        // then - 같은 사용자가 다시 발급 시도하면 예외 발생
        assertThrows(CustomException.class, () ->
            couponService.issueCoupon(userId, savedCoupon.getId())
        );
    }

    @Test
    @DisplayName("수량이 소진된 쿠폰은 발급할 수 없다")
    void issueCoupon_Exhausted() {
        // given
        Coupon coupon = Coupon.createFixed(
            "선착순 쿠폰",
            5000L,
            2,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        // when
        couponService.issueCoupon(1L, savedCoupon.getId());
        couponService.issueCoupon(2L, savedCoupon.getId());

        // then
        assertThrows(CustomException.class, () ->
            couponService.issueCoupon(3L, savedCoupon.getId())
        );
    }

    @Test
    @DisplayName("동시에 100명이 선착순 쿠폰 발급을 시도해도 정확히 100개만 발급된다")
    void issueCoupon_Concurrency() throws InterruptedException {
        // given
        Coupon coupon = Coupon.createFixed(
            "선착순 100개 한정 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 200명이 동시에 발급 시도
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(userId, savedCoupon.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 100개만 발급되어야 함
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(100);

        // 쿠폰의 발급 수량도 100개여야 함
        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).get();
        assertThat(issuedCoupon.getIssuedQuantity()).isEqualTo(100);
        assertThat(issuedCoupon.canIssue()).isFalse();
    }

    @Test
    @DisplayName("만료된 쿠폰은 발급할 수 없다")
    void issueCoupon_Expired() {
        // given
        Coupon coupon = Coupon.createFixed(
            "만료된 쿠폰",
            5000L,
            100,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(1),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        // when & then
        assertThrows(CustomException.class, () ->
            couponService.issueCoupon(1L, savedCoupon.getId())
        );
    }

    @Test
    @DisplayName("비활성 쿠폰은 발급할 수 없다")
    void issueCoupon_Inactive() {
        // given
        Coupon coupon = Coupon.createFixed(
            "비활성 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);
        savedCoupon.deactivate();
        couponRepository.save(savedCoupon);

        // when & then
        assertThrows(CustomException.class, () ->
            couponService.issueCoupon(1L, savedCoupon.getId())
        );
    }

    @Test
    @DisplayName("사용자의 쿠폰 목록을 조회할 수 있다")
    void getUserCoupons() {
        // given
        Coupon coupon1 = Coupon.createFixed("쿠폰1", 5000L, 100,
            LocalDate.now(), LocalDate.now().plusDays(30), 10000L);
        Coupon coupon2 = Coupon.createFixed("쿠폰2", 3000L, 100,
            LocalDate.now(), LocalDate.now().plusDays(30), 5000L);

        Coupon savedCoupon1 = couponRepository.save(coupon1);
        Coupon savedCoupon2 = couponRepository.save(coupon2);

        Long userId = 1L;
        couponService.issueCoupon(userId, savedCoupon1.getId());
        couponService.issueCoupon(userId, savedCoupon2.getId());

        // when
        List<UserCoupon> userCoupons = couponService.getUserCoupons(userId);

        // then
        assertThat(userCoupons).hasSize(2);
    }

    @Test
    @DisplayName("사용 가능한 쿠폰 목록만 조회할 수 있다")
    void getAvailableUserCoupons() {
        // given
        Coupon validCoupon = Coupon.createFixed("유효한 쿠폰", 5000L, 100,
            LocalDate.now(), LocalDate.now().plusDays(30), 10000L);
        Coupon expiredCoupon = Coupon.createFixed("만료된 쿠폰", 3000L, 100,
            LocalDate.now().minusDays(30), LocalDate.now().minusDays(1), 5000L);

        Coupon savedValidCoupon = couponRepository.save(validCoupon);
        Coupon savedExpiredCoupon = couponRepository.save(expiredCoupon);

        Long userId = 1L;
        couponService.issueCoupon(userId, savedValidCoupon.getId());

        UserCoupon expiredUserCoupon = UserCoupon.create(userId, savedExpiredCoupon);
        userCouponRepository.save(expiredUserCoupon);

        // when
        List<UserCoupon> availableCoupons = couponService.getAvailableUserCoupons(userId);

        // then
        assertThat(availableCoupons).hasSize(1);
        assertThat(availableCoupons.get(0).getCoupon().getName()).isEqualTo("유효한 쿠폰");
    }
}
