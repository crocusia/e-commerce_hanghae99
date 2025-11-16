package com.example.ecommerce.coupon.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.domain.vo.CouponQuantity;
import com.example.ecommerce.coupon.domain.vo.DiscountValue;
import com.example.ecommerce.coupon.domain.vo.ValidPeriod;
import com.example.ecommerce.coupon.dto.UserCouponResponse;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.product.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCouponService 단위 테스트")
class UserCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private UserCouponService userCouponService;

    private Long testUserId;
    private Long testCouponId;
    private Long testUserCouponId;
    private Coupon testCoupon;
    private UserCoupon testUserCoupon;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testCouponId = 1L;
        testUserCouponId = 1L;

        testCoupon = createTestCoupon(testCouponId, "테스트 쿠폰", 5000L, 100);
        testUserCoupon = createTestUserCoupon(testUserCouponId, testUserId, testCoupon,
            LocalDateTime.now().plusDays(30));
    }

    private Coupon createTestCoupon(Long id, String name, Long discountPrice, int totalQuantity) {
        return Coupon.builder()
            .id(id)
            .name(name)
            .discountValue(DiscountValue.fixed(discountPrice))
            .quantity(CouponQuantity.of(totalQuantity))
            .validPeriod(ValidPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30)))
            .minOrderAmount(Money.of(10000L))
            .build();
    }

    private UserCoupon createTestUserCoupon(Long id, Long userId, Coupon coupon, LocalDateTime expiresAt) {
        return UserCoupon.builder()
            .id(id)
            .userId(userId)
            .coupon(coupon)
            .expiresAt(expiresAt)
            .build();
    }

    private UserCoupon createExpiredUserCoupon(Long id, Long userId, Coupon coupon) {
        return UserCoupon.builder()
            .id(id)
            .userId(userId)
            .coupon(coupon)
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    }

    private void assertThrowsCustomException(ErrorCode expectedErrorCode, Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(expectedErrorCode);
    }

    @Nested
    @DisplayName("쿠폰 발급 테스트")
    class IssueCouponTest {

        @Test
        @DisplayName("쿠폰을 정상적으로 발급받을 수 있다")
        void issueCoupon_Success() {
            // given
            given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(testCoupon);
            given(userCouponRepository.findByUserIdAndCouponId(testUserId, testCouponId)).willReturn(
                Optional.empty());
            given(userCouponRepository.save(any(UserCoupon.class))).willReturn(testUserCoupon);

            // when
            UserCouponResponse result = userCouponService.issueCoupon(testCouponId, testUserId);

            // then - 행위 검증 위주
            assertThat(result).isNotNull();

            then(couponRepository).should().findByIdOrElseThrow(testCouponId);
            then(userCouponRepository).should().findByUserIdAndCouponId(testUserId, testCouponId);
            then(userCouponRepository).should().save(any(UserCoupon.class));
        }

        @Test
        @DisplayName("이미 발급된 쿠폰 재발급 시도 시 예외 발생")
        void issueCoupon_AlreadyIssued() {
            // given
            given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(testCoupon);
            // 같은 사용자가 같은 쿠폰을 이미 받은 상태
            given(userCouponRepository.findByUserIdAndCouponId(testUserId, testCouponId)).willReturn(
                Optional.ofNullable(testUserCoupon));

            // when & then
            assertThrowsCustomException(
                ErrorCode.COUPON_ALREADY_ISSUED,
                () -> userCouponService.issueCoupon(testCouponId, testUserId)
            );

            then(couponRepository).should().findByIdOrElseThrow(testCouponId);
            then(userCouponRepository).should().findByUserIdAndCouponId(testUserId, testCouponId);
            then(userCouponRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("사용 가능한 쿠폰 조회 테스트")
    class GetAvailableUserCouponsTest {

        @Test
        @DisplayName("사용 가능한 쿠폰만 조회할 수 있다")
        void getAvailableUserCoupons_Success() {
            // given
            UserCoupon validUserCoupon = createTestUserCoupon(1L, testUserId, testCoupon,
                LocalDateTime.now().plusDays(30));

            Coupon expiredCoupon = createTestCoupon(2L, "만료된 쿠폰", 3000L, 50);
            UserCoupon expiredUserCoupon = createExpiredUserCoupon(2L, testUserId, expiredCoupon);

            List<UserCoupon> userCoupons = Arrays.asList(validUserCoupon, expiredUserCoupon);

            given(userCouponRepository.findByUserId(testUserId)).willReturn(userCoupons);
            given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(testCoupon);

            // when
            List<UserCouponResponse> result = userCouponService.getAvailableUserCoupons(testUserId);

            // then
            assertThat(result).hasSize(1);

            then(userCouponRepository).should().findByUserId(testUserId);
            then(couponRepository).should().findByIdOrElseThrow(testCouponId);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 테스트")
    class UseCouponTest {

        @Test
        @DisplayName("쿠폰을 정상적으로 사용할 수 있다")
        void useCoupon_Success() {
            // given
            given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(testUserCoupon);
            given(userCouponRepository.save(any(UserCoupon.class))).willReturn(testUserCoupon);
            given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(testCoupon);

            // when
            UserCouponResponse result = userCouponService.useCoupon(testUserCouponId);

            // then
            assertThat(result).isNotNull();

            then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
            then(userCouponRepository).should().save(any(UserCoupon.class));
            then(couponRepository).should().findByIdOrElseThrow(testCouponId);
        }
    }

    @Nested
    @DisplayName("사용자 쿠폰 상세 조회 테스트")
    class GetUserCouponTest {

        @Test
        @DisplayName("사용자 쿠폰 상세를 조회할 수 있다")
        void getUserCoupon_Success() {
            // given
            given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(testUserCoupon);
            given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(testCoupon);

            // when
            UserCouponResponse result = userCouponService.getUserCoupon(testUserCouponId);

            // then
            assertThat(result).isNotNull();

            then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
            then(couponRepository).should().findByIdOrElseThrow(testCouponId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 쿠폰 조회 시 예외 발생")
        void getUserCoupon_NotFound() {
            // given
            given(userCouponRepository.findByIdOrElseThrow(testUserCouponId))
                .willThrow(new CustomException(ErrorCode.COUPON_NOT_FOUND));

            // when & then
            assertThrowsCustomException(
                ErrorCode.COUPON_NOT_FOUND,
                () -> userCouponService.getUserCoupon(testUserCouponId)
            );

            then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
            then(couponRepository).should(never()).findByIdOrElseThrow(any());
        }

        @Test
        @DisplayName("쿠폰 정보가 존재하지 않으면 예외 발생")
        void getUserCoupon_CouponNotFound() {
            // given
            given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(testUserCoupon);
            given(couponRepository.findByIdOrElseThrow(testCouponId))
                .willThrow(new CustomException(ErrorCode.COUPON_NOT_FOUND));

            // when & then
            assertThrowsCustomException(
                ErrorCode.COUPON_NOT_FOUND,
                () -> userCouponService.getUserCoupon(testUserCouponId)
            );

            then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
            then(couponRepository).should().findByIdOrElseThrow(testCouponId);
        }
    }
}
