package com.example.ecommerce.service.coupon;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.InMemoryCouponRepository;
import com.example.ecommerce.coupon.repository.InMemoryUserCouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.coupon.service.UserCouponService;
import com.example.ecommerce.coupon.service.UserCouponService.IssueCouponInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("쿠폰 동시성 통합 테스트")
class CouponConcurrencyIntegrationTest {

    private UserCouponService userCouponService;
    private CouponRepository couponRepository;
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        couponRepository = new InMemoryCouponRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        userCouponService = new UserCouponService(couponRepository, userCouponRepository);

        ((InMemoryCouponRepository) couponRepository).clear();
        ((InMemoryUserCouponRepository) userCouponRepository).clear();
    }

    // === 동시성 제어 테스트 ===

    @Test
    @DisplayName("100명이 동시에 선착순 100개 쿠폰 발급 시도 - 정확히 100개만 발급")
    void issueCoupon_Concurrency_ExactLimit() throws InterruptedException {
        // given
        int totalQuantity = 100;
        int threadCount = 100;

        Coupon coupon = Coupon.createFixed(
            "선착순 100개 한정 쿠폰",
            5000L,
            totalQuantity,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 발급 시도
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    userCouponService.issueCoupon(new IssueCouponInput(userId, savedCoupon.getId()));
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
        assertThat(failCount.get()).isEqualTo(0);

        // 쿠폰의 발급 수량도 100개여야 함
        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).get();
        assertThat(issuedCoupon.getIssuedQuantity()).isEqualTo(100);
        assertThat(issuedCoupon.canIssue()).isFalse();
    }

    @Test
    @DisplayName("200명이 동시에 선착순 100개 쿠폰 발급 시도 - 정확히 100개만 발급, 100명 실패")
    void issueCoupon_Concurrency_OverLimit() throws InterruptedException {
        // given
        int totalQuantity = 100;
        int threadCount = 200;

        Coupon coupon = Coupon.createFixed(
            "선착순 100개 한정 쿠폰",
            5000L,
            totalQuantity,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 200명이 동시에 발급 시도
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    userCouponService.issueCoupon(new IssueCouponInput(userId, savedCoupon.getId()));
                    successCount.incrementAndGet();
                } catch (CustomException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 100개만 발급, 100명 실패
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(100);

        // 쿠폰의 발급 수량도 100개여야 함
        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).get();
        assertThat(issuedCoupon.getIssuedQuantity()).isEqualTo(100);
        assertThat(issuedCoupon.canIssue()).isFalse();
    }

    @Test
    @DisplayName("1000명이 동시에 선착순 10개 쿠폰 발급 시도 - 정확히 10개만 발급")
    void issueCoupon_Concurrency_HighContention() throws InterruptedException {
        // given
        int totalQuantity = 10;
        int threadCount = 1000;

        Coupon coupon = Coupon.createFixed(
            "선착순 10개 한정 쿠폰",
            5000L,
            totalQuantity,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 1000명이 동시에 발급 시도
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    userCouponService.issueCoupon(new IssueCouponInput(userId, savedCoupon.getId()));
                    successCount.incrementAndGet();
                } catch (CustomException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 10개만 발급되어야 함
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(990);

        // 쿠폰의 발급 수량도 10개여야 함
        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).get();
        assertThat(issuedCoupon.getIssuedQuantity()).isEqualTo(10);
        assertThat(issuedCoupon.canIssue()).isFalse();
    }

    @Test
    @DisplayName("서로 다른 쿠폰에 대한 동시 발급은 독립적으로 처리됨")
    void issueCoupon_Concurrency_MultipleCoupons() throws InterruptedException {
        // given
        Coupon coupon1 = Coupon.createFixed("쿠폰1", 5000L, 50,
            LocalDate.now(), LocalDate.now().plusDays(30), 10000L);
        Coupon coupon2 = Coupon.createFixed("쿠폰2", 3000L, 50,
            LocalDate.now(), LocalDate.now().plusDays(30), 5000L);

        Coupon savedCoupon1 = couponRepository.save(coupon1);
        Coupon savedCoupon2 = couponRepository.save(coupon2);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when - 100명이 각각 다른 쿠폰 발급 (50명씩)
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            final Long couponId = (i <= 50) ? savedCoupon1.getId() : savedCoupon2.getId();
            executorService.submit(() -> {
                try {
                    userCouponService.issueCoupon(new IssueCouponInput(userId, couponId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 각 쿠폰이 독립적으로 50개씩 발급되어야 함
        assertThat(successCount.get()).isEqualTo(100);

        Coupon issuedCoupon1 = couponRepository.findById(savedCoupon1.getId()).get();
        Coupon issuedCoupon2 = couponRepository.findById(savedCoupon2.getId()).get();

        assertThat(issuedCoupon1.getIssuedQuantity()).isEqualTo(50);
        assertThat(issuedCoupon2.getIssuedQuantity()).isEqualTo(50);
        assertThat(issuedCoupon1.canIssue()).isFalse();
        assertThat(issuedCoupon2.canIssue()).isFalse();
    }

    @Test
    @DisplayName("동일 사용자가 같은 쿠폰을 여러 번 발급 시도해도 1번만 성공")
    void issueCoupon_Concurrency_SameUserMultipleAttempts() throws InterruptedException {
        // given
        Coupon coupon = Coupon.createFixed(
            "테스트 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        Long userId = 1L;
        int attemptCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch latch = new CountDownLatch(attemptCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 사용자가 10번 동시 발급 시도
        for (int i = 0; i < attemptCount; i++) {
            executorService.submit(() -> {
                try {
                    userCouponService.issueCoupon(new IssueCouponInput(userId, savedCoupon.getId()));
                    successCount.incrementAndGet();
                } catch (CustomException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 1번만 성공, 9번 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);

        // 쿠폰의 발급 수량도 1개여야 함
        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).get();
        assertThat(issuedCoupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("수량 1개 쿠폰에 대한 극한의 경쟁 상황 테스트")
    void issueCoupon_Concurrency_SingleCouponHighContention() throws InterruptedException {
        // given
        Coupon coupon = Coupon.createFixed(
            "초특가 1개 한정 쿠폰",
            10000L,
            1,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            20000L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        int threadCount = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 500명이 1개 쿠폰 쟁탈전
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    userCouponService.issueCoupon(new IssueCouponInput(userId, savedCoupon.getId()));
                    successCount.incrementAndGet();
                } catch (CustomException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 1명만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(499);

        // 쿠폰의 발급 수량도 1개여야 함
        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).get();
        assertThat(issuedCoupon.getIssuedQuantity()).isEqualTo(1);
        assertThat(issuedCoupon.canIssue()).isFalse();
    }

    @Test
    @DisplayName("순차 발급과 동시 발급 결과가 동일함을 검증")
    void issueCoupon_Concurrency_ConsistencyCheck() throws InterruptedException {
        // given
        int totalQuantity = 50;

        // 순차 발급
        Coupon sequentialCoupon = Coupon.createFixed(
            "순차 발급 쿠폰",
            5000L,
            totalQuantity,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedSequentialCoupon = couponRepository.save(sequentialCoupon);

        for (int i = 1; i <= totalQuantity; i++) {
            userCouponService.issueCoupon(new IssueCouponInput((long) i, savedSequentialCoupon.getId()));
        }

        // 동시 발급
        Coupon concurrentCoupon = Coupon.createFixed(
            "동시 발급 쿠폰",
            5000L,
            totalQuantity,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        Coupon savedConcurrentCoupon = couponRepository.save(concurrentCoupon);

        ExecutorService executorService = Executors.newFixedThreadPool(totalQuantity);
        CountDownLatch latch = new CountDownLatch(totalQuantity);

        for (int i = 1; i <= totalQuantity; i++) {
            final long userId = 100L + i; // 다른 사용자 ID 사용
            executorService.submit(() -> {
                try {
                    userCouponService.issueCoupon(new IssueCouponInput(userId, savedConcurrentCoupon.getId()));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 순차와 동시 발급 결과가 동일해야 함
        Coupon sequentialResult = couponRepository.findById(savedSequentialCoupon.getId()).get();
        Coupon concurrentResult = couponRepository.findById(savedConcurrentCoupon.getId()).get();

        assertThat(sequentialResult.getIssuedQuantity()).isEqualTo(concurrentResult.getIssuedQuantity());
        assertThat(sequentialResult.canIssue()).isEqualTo(concurrentResult.canIssue());
        assertThat(sequentialResult.getIssuedQuantity()).isEqualTo(totalQuantity);
    }
}
