package com.example.ecommerce.service.coupon;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.coupon.service.UserCouponService;
import com.example.ecommerce.coupon.service.UserCouponService.IssueCouponInput;
import com.example.ecommerce.coupon.service.UserCouponService.UserCouponOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCouponService 단위 테스트 (Mock)")
class UserCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private UserCouponService userCouponService;

    private Coupon testCoupon;
    private UserCoupon testUserCoupon;
    private Long testUserId;
    private Long testCouponId;
    private Long testUserCouponId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testCouponId = 1L;
        testUserCouponId = 1L;

        testCoupon = createTestCoupon("테스트 쿠폰", 5000L, 100);
        setId(testCoupon, testCouponId);

        testUserCoupon = UserCoupon.create(testUserId, testCoupon);
        setId(testUserCoupon, testUserCouponId);
    }

    // === 헬퍼 메서드 ===

    private Coupon createTestCoupon(String name, Long discountPrice, int quantity) {
        return Coupon.createFixed(
            name,
            discountPrice,
            quantity,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
    }

    private Coupon createExpiredCoupon(String name) {
        return Coupon.createFixed(
            name,
            5000L,
            100,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(1),
            10000L
        );
    }

    private Coupon createInactiveCoupon(String name) {
        Coupon coupon = createTestCoupon(name, 5000L, 100);
        coupon.deactivate();
        return coupon;
    }

    private void setId(Object entity, Long id) {
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private IssueCouponInput createInput(Long userId, Long couponId) {
        return new IssueCouponInput(userId, couponId);
    }

    // === 쿠폰 발급 테스트 ===

    @Test
    @DisplayName("쿠폰을 정상적으로 발급받을 수 있다")
    void issueCoupon_Success() {
        // given
        IssueCouponInput input = createInput(testUserId, testCouponId);

        given(couponRepository.findById(testCouponId)).willReturn(Optional.of(testCoupon));
        given(userCouponRepository.existsByUserIdAndCouponId(testUserId, testCouponId)).willReturn(false);
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(testUserCoupon);

        // when
        UserCouponOutput result = userCouponService.issueCoupon(input);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(testUserId);
        assertThat(result.couponId()).isEqualTo(testCouponId);

        then(couponRepository).should().findById(testCouponId);
        then(userCouponRepository).should().existsByUserIdAndCouponId(testUserId, testCouponId);
        then(couponRepository).should().save(testCoupon);
        then(userCouponRepository).should().save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 발급 시 예외 발생")
    void issueCoupon_CouponNotFound() {
        // given
        IssueCouponInput input = createInput(testUserId, testCouponId);
        given(couponRepository.findById(testCouponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userCouponService.issueCoupon(input))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.COUPON_NOT_FOUND.getMessage());

        then(couponRepository).should().findById(testCouponId);
        then(userCouponRepository).should(never()).existsByUserIdAndCouponId(any(), any());
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰 재발급 시도 시 예외 발생 (1인 1매)")
    void issueCoupon_AlreadyIssued() {
        // given
        IssueCouponInput input = createInput(testUserId, testCouponId);

        given(couponRepository.findById(testCouponId)).willReturn(Optional.of(testCoupon));
        given(userCouponRepository.existsByUserIdAndCouponId(testUserId, testCouponId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userCouponService.issueCoupon(input))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.COUPON_ALREADY_USED.getMessage());

        then(couponRepository).should().findById(testCouponId);
        then(userCouponRepository).should().existsByUserIdAndCouponId(testUserId, testCouponId);
        then(couponRepository).should(never()).save(any());
        then(userCouponRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("만료된 쿠폰 발급 시도 시 예외 발생")
    void issueCoupon_ExpiredCoupon() {
        // given
        Coupon expiredCoupon = createExpiredCoupon("만료된 쿠폰");
        setId(expiredCoupon, testCouponId);
        IssueCouponInput input = createInput(testUserId, testCouponId);

        given(couponRepository.findById(testCouponId)).willReturn(Optional.of(expiredCoupon));
        given(userCouponRepository.existsByUserIdAndCouponId(testUserId, testCouponId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userCouponService.issueCoupon(input))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.COUPON_EXPIRED.getMessage());

        then(couponRepository).should().findById(testCouponId);
        then(userCouponRepository).should().existsByUserIdAndCouponId(testUserId, testCouponId);
    }

    @Test
    @DisplayName("비활성 쿠폰 발급 시도 시 예외 발생")
    void issueCoupon_InactiveCoupon() {
        // given
        Coupon inactiveCoupon = createInactiveCoupon("비활성 쿠폰");
        setId(inactiveCoupon, testCouponId);
        IssueCouponInput input = createInput(testUserId, testCouponId);

        given(couponRepository.findById(testCouponId)).willReturn(Optional.of(inactiveCoupon));
        given(userCouponRepository.existsByUserIdAndCouponId(testUserId, testCouponId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userCouponService.issueCoupon(input))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.COUPON_NOT_AVAILABLE.getMessage());
    }

    @Test
    @DisplayName("수량이 소진된 쿠폰 발급 시도 시 예외 발생")
    void issueCoupon_ExhaustedCoupon() {
        // given
        Coupon exhaustedCoupon = createTestCoupon("소진된 쿠폰", 5000L, 1);
        exhaustedCoupon.issue(); // 수량 소진
        setId(exhaustedCoupon, testCouponId);
        IssueCouponInput input = createInput(testUserId, testCouponId);

        given(couponRepository.findById(testCouponId)).willReturn(Optional.of(exhaustedCoupon));
        given(userCouponRepository.existsByUserIdAndCouponId(testUserId, testCouponId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userCouponService.issueCoupon(input))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.COUPON_NOT_AVAILABLE.getMessage());
    }

    // === 사용자 쿠폰 조회 테스트 ===

    @Test
    @DisplayName("사용자의 쿠폰 목록을 조회할 수 있다")
    void getUserCoupons() {
        // given
        UserCoupon userCoupon1 = UserCoupon.create(testUserId, testCoupon);
        UserCoupon userCoupon2 = UserCoupon.create(testUserId, testCoupon);
        List<UserCoupon> userCoupons = Arrays.asList(userCoupon1, userCoupon2);

        given(userCouponRepository.findByUserId(testUserId)).willReturn(userCoupons);

        // when
        List<UserCouponOutput> result = userCouponService.getUserCoupons(testUserId);

        // then
        assertThat(result).hasSize(2);
        then(userCouponRepository).should().findByUserId(testUserId);
    }

    @Test
    @DisplayName("사용 가능한 쿠폰만 조회할 수 있다")
    void getAvailableUserCoupons() {
        // given
        UserCoupon validCoupon = UserCoupon.create(testUserId, testCoupon);

        Coupon expiredCoupon = createExpiredCoupon("만료된 쿠폰");
        UserCoupon expiredUserCoupon = UserCoupon.create(testUserId, expiredCoupon);

        List<UserCoupon> userCoupons = Arrays.asList(validCoupon, expiredUserCoupon);

        given(userCouponRepository.findByUserId(testUserId)).willReturn(userCoupons);

        // when
        List<UserCouponOutput> result = userCouponService.getAvailableUserCoupons(testUserId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).couponName()).isEqualTo("테스트 쿠폰");
        then(userCouponRepository).should().findByUserId(testUserId);
    }

    @Test
    @DisplayName("사용자 쿠폰 상세를 조회할 수 있다")
    void getUserCoupon() {
        // given
        given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(testUserCoupon);

        // when
        UserCouponOutput result = userCouponService.getUserCoupon(testUserCouponId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(testUserCouponId);
        assertThat(result.userId()).isEqualTo(testUserId);
        then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
    }

    // === 쿠폰 사용 테스트 ===

    @Test
    @DisplayName("쿠폰을 사용할 수 있다")
    void useCoupon() {
        // given
        given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(testUserCoupon);
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(testUserCoupon);

        // when
        UserCouponOutput result = userCouponService.useCoupon(testUserCouponId);

        // then
        assertThat(result).isNotNull();
        then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
        then(userCouponRepository).should().save(testUserCoupon);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 쿠폰 사용 시 예외 발생")
    void useCoupon_NotFound() {
        // given
        given(userCouponRepository.findByIdOrElseThrow(testUserCouponId))
            .willThrow(new CustomException(ErrorCode.COUPON_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> userCouponService.useCoupon(testUserCouponId))
            .isInstanceOf(CustomException.class);

        then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
        then(userCouponRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미 사용한 쿠폰을 다시 사용하려고 하면 예외 발생")
    void useCoupon_AlreadyUsed() {
        // given
        testUserCoupon.use(); // 쿠폰 사용 처리
        given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(testUserCoupon);

        // when & then
        assertThatThrownBy(() -> userCouponService.useCoupon(testUserCouponId))
            .isInstanceOf(CustomException.class);

        then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
        then(userCouponRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("만료된 쿠폰을 사용하려고 하면 예외 발생")
    void useCoupon_ExpiredCoupon() {
        // given
        Coupon expiredCoupon = createExpiredCoupon("만료된 쿠폰");
        UserCoupon expiredUserCoupon = UserCoupon.create(testUserId, expiredCoupon);
        setId(expiredUserCoupon, testUserCouponId);

        given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(expiredUserCoupon);

        // when & then
        assertThatThrownBy(() -> userCouponService.useCoupon(testUserCouponId))
            .isInstanceOf(CustomException.class);

        then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
        then(userCouponRepository).should(never()).save(any());
    }
}
