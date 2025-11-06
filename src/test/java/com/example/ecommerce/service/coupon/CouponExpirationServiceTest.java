package com.example.ecommerce.service.coupon;

import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.coupon.service.CouponExpirationService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponExpirationService 단위 테스트 (Mock)")
class CouponExpirationServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponExpirationService couponExpirationService;

    private Coupon expiredCoupon;
    private Coupon validCoupon;

    @BeforeEach
    void setUp() {
        // 간단한 Mock 객체 설정 (만료 여부만 중요하다고 가정)
        expiredCoupon = mock(Coupon.class);
        given(expiredCoupon.getId()).willReturn(100L); // 만료될 쿠폰 ID

        validCoupon = mock(Coupon.class);
        given(validCoupon.getId()).willReturn(200L); // 만료되지 않을 쿠폰 ID
    }

    // === 쿠폰 만료 처리 테스트 ===
    @Test
    @DisplayName("만료된 쿠폰들을 일괄 처리하고 만료된 개수를 반환한다")
    void expireExpiredCoupons_shouldExpireUnusedCoupons() {
        // GIVEN
        List<Long> expiredCouponIds = Arrays.asList(expiredCoupon.getId());
        given(couponRepository.findExpiredCouponIds()).willReturn(expiredCouponIds);

        UserCoupon expiredUserCoupon1 = UserCoupon.create(1L, expiredCoupon);
        UserCoupon expiredUserCoupon2 = UserCoupon.create(2L, expiredCoupon);

        List<UserCoupon> unusedExpiredCoupons = Arrays.asList(
            expiredUserCoupon1,
            expiredUserCoupon2
        );

        given(userCouponRepository.findByCouponIdsAndUnusedStatus(anyList()))
            .willReturn(unusedExpiredCoupons);


        // WHEN
        int expiredCount = couponExpirationService.expireExpiredCoupons();

        // THEN
        // 1. 반환된 개수 검증
        assertThat(expiredCount).isEqualTo(2);

        // 2. 1단계와 2단계 조회 메서드 호출 검증
        then(couponRepository).should(times(1)).findExpiredCouponIds();
        then(userCouponRepository).should(times(1)).findByCouponIdsAndUnusedStatus(expiredCouponIds);

        // 3. 만료 처리 (UserCoupon::expire) 및 저장 (userCouponRepository::save) 검증
        // 3-1. 각 UserCoupon 객체에 대해 expire() 메서드가 호출되었다고 가정하고 만료 상태를 검증
        assertThat(expiredUserCoupon1.isExpired()).isTrue();
        assertThat(expiredUserCoupon2.isExpired()).isTrue();

        // 3-2. 각 만료된 쿠폰에 대해 save 메서드가 호출되었는지 검증
        then(userCouponRepository).should(times(1)).save(expiredUserCoupon1);
        then(userCouponRepository).should(times(1)).save(expiredUserCoupon2);
        then(userCouponRepository).should(times(2)).save(any(UserCoupon.class)); // 총 2번 호출 검증
    }

    @Test
    @DisplayName("만료된 쿠폰이 없으면 0을 반환한다")
    void expireExpiredCoupons_shouldReturnZeroWhenNoExpiredCoupons() {
        // GIVEN
        // 1단계 Mocking: 만료된 쿠폰 ID가 없음
        given(couponRepository.findExpiredCouponIds()).willReturn(Collections.emptyList());

        // WHEN
        int expiredCount = couponExpirationService.expireExpiredCoupons();

        // THEN
        assertThat(expiredCount).isEqualTo(0);

        // 만료된 쿠폰이 없으므로 2단계 조회는 호출되지 않아야 함
        then(userCouponRepository).should(never()).findByCouponIdsAndUnusedStatus(anyList());
        then(userCouponRepository).should(never()).save(any(UserCoupon.class));
    }
}
