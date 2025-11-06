package com.example.ecommerce.service.coupon;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.service.CouponService;
import com.example.ecommerce.coupon.service.CouponService.CouponDetailOutput;
import com.example.ecommerce.coupon.service.CouponService.CouponOutput;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService 단위 테스트 (Mock)")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon testCoupon;
    private Long testCouponId;

    @BeforeEach
    void setUp() {
        testCouponId = 1L;
        testCoupon = createTestCoupon("테스트 쿠폰", 5000L, 100);
        setId(testCoupon, testCouponId);
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

    private Coupon createPercentageCoupon(String name, Double discountRate, int quantity) {
        return Coupon.createPercentage(
            name,
            discountRate,
            quantity,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
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

    // === 쿠폰 목록 조회 테스트 ===

    @Test
    @DisplayName("전체 쿠폰 목록을 조회할 수 있다")
    void getAllCoupons() {
        // given
        Coupon coupon1 = createTestCoupon("쿠폰1", 5000L, 100);
        Coupon coupon2 = createTestCoupon("쿠폰2", 3000L, 50);
        Coupon coupon3 = createPercentageCoupon("쿠폰3", 10.0, 200);

        List<Coupon> coupons = Arrays.asList(coupon1, coupon2, coupon3);
        given(couponRepository.findAll()).willReturn(coupons);

        // when
        List<CouponOutput> result = couponService.getAllCoupons();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).name()).isEqualTo("쿠폰1");
        assertThat(result.get(1).name()).isEqualTo("쿠폰2");
        assertThat(result.get(2).name()).isEqualTo("쿠폰3");

        then(couponRepository).should().findAll();
    }

    @Test
    @DisplayName("쿠폰 목록이 비어있으면 빈 리스트를 반환한다")
    void getAllCoupons_Empty() {
        // given
        given(couponRepository.findAll()).willReturn(List.of());

        // when
        List<CouponOutput> result = couponService.getAllCoupons();

        // then
        assertThat(result).isEmpty();
        then(couponRepository).should().findAll();
    }

    @Test
    @DisplayName("쿠폰 목록 조회 시 남은 수량이 정확히 계산된다")
    void getAllCoupons_RemainingQuantity() {
        // given
        Coupon coupon = createTestCoupon("테스트 쿠폰", 5000L, 10);
        // 3개 발급
        coupon.issue();
        coupon.issue();
        coupon.issue();

        given(couponRepository.findAll()).willReturn(List.of(coupon));

        // when
        List<CouponOutput> result = couponService.getAllCoupons();

        // then
        assertThat(result).hasSize(1);
        CouponOutput output = result.get(0);
        assertThat(output.totalQuantity()).isEqualTo(10);
        assertThat(output.issuedQuantity()).isEqualTo(3);
        assertThat(output.remainingQuantity()).isEqualTo(7);
    }

    // === 쿠폰 상세 조회 테스트 ===

    @Test
    @DisplayName("쿠폰 상세 정보를 조회할 수 있다")
    void getCoupon() {
        // given
        given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(testCoupon);

        // when
        CouponDetailOutput result = couponService.getCoupon(testCouponId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(testCouponId);
        assertThat(result.name()).isEqualTo("테스트 쿠폰");
        assertThat(result.discountPrice()).isEqualTo(5000L);
        assertThat(result.totalQuantity()).isEqualTo(100);
        assertThat(result.issuedQuantity()).isEqualTo(0);
        assertThat(result.remainingQuantity()).isEqualTo(100);
        assertThat(result.canIssue()).isTrue();
        assertThat(result.isValid()).isTrue();

        then(couponRepository).should().findByIdOrElseThrow(testCouponId);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 조회 시 예외 발생")
    void getCoupon_NotFound() {
        // given
        given(couponRepository.findByIdOrElseThrow(testCouponId))
            .willThrow(new CustomException(ErrorCode.COUPON_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> couponService.getCoupon(testCouponId))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.COUPON_NOT_FOUND.getMessage());

        then(couponRepository).should().findByIdOrElseThrow(testCouponId);
    }

    @Test
    @DisplayName("정액 할인 쿠폰 정보를 정확히 조회한다")
    void getCoupon_FixedDiscount() {
        // given
        Coupon fixedCoupon = createTestCoupon("정액 할인 쿠폰", 5000L, 100);
        setId(fixedCoupon, testCouponId);

        given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(fixedCoupon);

        // when
        CouponDetailOutput result = couponService.getCoupon(testCouponId);

        // then
        assertThat(result.discountPrice()).isEqualTo(5000L);
        assertThat(result.discountRate()).isNull();
    }

    @Test
    @DisplayName("정율 할인 쿠폰 정보를 정확히 조회한다")
    void getCoupon_PercentageDiscount() {
        // given
        Coupon percentageCoupon = createPercentageCoupon("정율 할인 쿠폰", 10.0, 100);
        setId(percentageCoupon, testCouponId);

        given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(percentageCoupon);

        // when
        CouponDetailOutput result = couponService.getCoupon(testCouponId);

        // then
        assertThat(result.discountPrice()).isNull();
        assertThat(result.discountRate()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("발급 완료된 쿠폰의 발급 가능 여부를 확인할 수 있다")
    void getCoupon_IssuedCoupon() {
        // given
        Coupon coupon = createTestCoupon("소진 쿠폰", 5000L, 1);
        coupon.issue(); // 수량 소진
        setId(coupon, testCouponId);

        given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(coupon);

        // when
        CouponDetailOutput result = couponService.getCoupon(testCouponId);

        // then
        assertThat(result.issuedQuantity()).isEqualTo(1);
        assertThat(result.remainingQuantity()).isEqualTo(0);
        assertThat(result.canIssue()).isFalse();
    }

    @Test
    @DisplayName("만료된 쿠폰의 유효성을 확인할 수 있다")
    void getCoupon_ExpiredCoupon() {
        // given
        Coupon expiredCoupon = Coupon.createFixed(
            "만료된 쿠폰",
            5000L,
            100,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(1),
            10000L
        );
        setId(expiredCoupon, testCouponId);

        given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(expiredCoupon);

        // when
        CouponDetailOutput result = couponService.getCoupon(testCouponId);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.canIssue()).isFalse();
    }

    @Test
    @DisplayName("비활성 쿠폰 상태를 확인할 수 있다")
    void getCoupon_InactiveCoupon() {
        // given
        testCoupon.deactivate();
        given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(testCoupon);

        // when
        CouponDetailOutput result = couponService.getCoupon(testCouponId);

        // then
        assertThat(result.status().name()).isEqualTo("INACTIVE");
    }
}
