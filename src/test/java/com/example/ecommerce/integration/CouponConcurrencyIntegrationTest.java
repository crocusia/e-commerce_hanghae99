package com.example.ecommerce.integration;

import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.vo.CouponQuantity;
import com.example.ecommerce.coupon.domain.vo.DiscountValue;
import com.example.ecommerce.coupon.domain.vo.ValidPeriod;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.InMemoryCouponRepository;
import com.example.ecommerce.coupon.repository.InMemoryUserCouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.coupon.service.UserCouponService;
import com.example.ecommerce.product.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("쿠폰 동시성 통합 테스트")
class CouponConcurrencyIntegrationTest {

    @Autowired
    private UserCouponService userCouponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CouponRepository couponRepository() {
            return new InMemoryCouponRepository();
        }

        @Bean
        @Primary
        public UserCouponRepository userCouponRepository() {
            return new InMemoryUserCouponRepository();
        }
    }

    @BeforeEach
    void setUp() {
        ((InMemoryCouponRepository) couponRepository).clear();
        ((InMemoryUserCouponRepository) userCouponRepository).clear();
    }

    private Coupon createCoupon(String name, Long discountPrice, int totalQuantity, int validDays) {
        return Coupon.builder()
            .name(name)
            .discountValue(DiscountValue.fixed(Money.of(discountPrice)))
            .quantity(CouponQuantity.of(totalQuantity))
            .validPeriod(ValidPeriod.of(LocalDate.now(), LocalDate.now().plusDays(validDays)))
            .minOrderAmount(Money.of(10000L))
            .build();
    }

    private int getIssuedQuantity(Coupon coupon) {
        return coupon.getQuantity().getIssuedQuantity();
    }

    @Test
    @DisplayName("200명이 동시에 선착순 100개 쿠폰 발급 시도 - 정확히 100개만 발급, 100명 실패")
    void issueCoupon_Concurrency_OverLimit() throws InterruptedException {
        // given
        int totalQuantity = 100;
        int threadCount = 200;

        Coupon coupon = createCoupon("선착순 100개 한정 쿠폰", 5000L, totalQuantity, 30);
        Coupon savedCoupon = couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    userCouponService.issueCoupon(savedCoupon.getId(), userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(100);

        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertThat(getIssuedQuantity(issuedCoupon)).isEqualTo(100);
        assertThat(issuedCoupon.canIssue()).isFalse();

        // 발급된 전체 쿠폰 개수 확인
        int totalIssuedCoupons = 0;
        for (long userId = 1; userId <= threadCount; userId++) {
            totalIssuedCoupons += userCouponRepository.findByUserId(userId).size();
        }
        assertThat(totalIssuedCoupons).isEqualTo(100);
    }

    @Test
    @DisplayName("서로 다른 쿠폰에 대한 동시 발급은 독립적으로 처리됨")
    void issueCoupon_Concurrency_MultipleCoupons() throws InterruptedException {
        // given
        Coupon coupon1 = createCoupon("쿠폰1", 5000L, 50, 30);
        Coupon coupon2 = createCoupon("쿠폰2", 3000L, 50, 30);

        Coupon savedCoupon1 = couponRepository.save(coupon1);
        Coupon savedCoupon2 = couponRepository.save(coupon2);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when - 100명이 각각 다른 쿠폰 발급 (50명씩)
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            final Long couponId = (i <= 50) ? savedCoupon1.getId() : savedCoupon2.getId();
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    userCouponService.issueCoupon(couponId, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패는 무시
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then - 각 쿠폰이 독립적으로 50개씩 발급되어야 함
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(100);

        Coupon issuedCoupon1 = couponRepository.findById(savedCoupon1.getId()).orElseThrow();
        Coupon issuedCoupon2 = couponRepository.findById(savedCoupon2.getId()).orElseThrow();

        assertThat(getIssuedQuantity(issuedCoupon1)).isEqualTo(50);
        assertThat(getIssuedQuantity(issuedCoupon2)).isEqualTo(50);
        assertThat(issuedCoupon1.canIssue()).isFalse();
        assertThat(issuedCoupon2.canIssue()).isFalse();
    }

    @Test
    @DisplayName("수량 1개 쿠폰에 대한 극한의 경쟁 상황 테스트")
    void issueCoupon_Concurrency_SingleCouponHighContention() throws InterruptedException {
        // given
        Coupon coupon = createCoupon("초특가 1개 한정 쿠폰", 10000L, 1, 30);
        Coupon savedCoupon = couponRepository.save(coupon);

        int threadCount = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 500명이 1개 쿠폰 쟁탈전
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    userCouponService.issueCoupon(savedCoupon.getId(), userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then - 정확히 1명만 성공
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(499);

        // 쿠폰의 발급 수량도 1개여야 함
        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertThat(getIssuedQuantity(issuedCoupon)).isEqualTo(1);
        assertThat(issuedCoupon.canIssue()).isFalse();
    }

    @Test
    @DisplayName("동시 발급 시 데이터 정합성 검증 - 발급된 쿠폰 수 = UserCoupon 레코드 수")
    void issueCoupon_Concurrency_DataIntegrity() throws InterruptedException {
        // given
        int totalQuantity = 50;
        int threadCount = 150;

        Coupon coupon = createCoupon("정합성 테스트 쿠폰", 5000L, totalQuantity, 30);
        Coupon savedCoupon = couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    userCouponService.issueCoupon(savedCoupon.getId(), userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패는 무시
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then - 데이터 정합성 검증
        assertThat(finished).isTrue();

        Coupon issuedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        int actualIssuedQuantity = getIssuedQuantity(issuedCoupon);

        // 모든 사용자의 쿠폰 개수 확인
        int userCouponCount = 0;
        for (long userId = 1; userId <= threadCount; userId++) {
            userCouponCount += userCouponRepository.findByUserId(userId).size();
        }

        // 발급 성공 횟수 = Coupon의 발급 수량 = UserCoupon 레코드 수
        assertThat(successCount.get()).isEqualTo(totalQuantity);
        assertThat(actualIssuedQuantity).isEqualTo(totalQuantity);
        assertThat(userCouponCount).isEqualTo(totalQuantity);
    }

    @Test
    @DisplayName("락 타임아웃 테스트 - 제한 시간 내에 모든 요청 처리")
    void issueCoupon_Concurrency_LockTimeout() throws InterruptedException {
        // given
        int totalQuantity = 20;
        int threadCount = 20;

        Coupon coupon = createCoupon("타임아웃 테스트 쿠폰", 5000L, totalQuantity, 30);
        Coupon savedCoupon = couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        // when
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    userCouponService.issueCoupon(savedCoupon.getId(), userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패는 무시
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        // then
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(totalQuantity);

        // 락 타임아웃(5초) * 스레드 수보다 훨씬 빠르게 처리되어야 함
        assertThat(elapsedTime).isLessThan(10000); // 10초 이내
    }
}
