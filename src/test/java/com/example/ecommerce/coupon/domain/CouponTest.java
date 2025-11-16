package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.coupon.domain.status.CouponStatus;
import com.example.ecommerce.coupon.domain.status.DiscountType;
import com.example.ecommerce.product.domain.vo.Money;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Coupon 도메인 테스트")
class CouponTest {

    @Test
    @DisplayName("정액 할인 쿠폰을 생성할 수 있다")
    void createFixedCoupon() {
        // when
        Coupon coupon = Coupon.createFixed(
            "5000원 할인 쿠폰",
            5000L,
            100,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            10000L
        );

        // then
        assertThat(coupon).isNotNull();
        assertThat(coupon.getName()).isEqualTo("5000원 할인 쿠폰");
        assertThat(coupon.getDiscountValue().getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(coupon.getDiscountValue().getDiscountPrice()).isEqualTo(5000L);
        assertThat(coupon.getQuantity().getTotalQuantity()).isEqualTo(100);
        assertThat(coupon.getQuantity().getIssuedQuantity()).isEqualTo(0);
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
    }

    @Test
    @DisplayName("정률 할인 쿠폰을 생성할 수 있다")
    void createPercentageCoupon() {
        // when
        Coupon coupon = Coupon.createPercentage(
            "10% 할인 쿠폰",
            10.0,
            50,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            20000L
        );

        // then
        assertThat(coupon).isNotNull();
        assertThat(coupon.getDiscountValue().getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(coupon.getDiscountValue().getDiscountRate()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("쿠폰을 발급할 수 있다")
    void issueCoupon() {
        // given
        Coupon coupon = Coupon.createFixed(
            "테스트 쿠폰",
            5000L,
            100,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            0L
        );

        // when
        coupon.issue();

        // then
        assertThat(coupon.getQuantity().getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("발급 수량이 모두 소진되면 더 이상 발급할 수 없다")
    void issueCoupon_Exhausted() {
        // given
        Coupon coupon = Coupon.createFixed(
            "테스트 쿠폰",
            5000L,
            2,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            0L
        );

        coupon.issue();
        coupon.issue();

        // when & then
        assertThrows(CustomException.class, coupon::issue);
    }

    @Test
    @DisplayName("발급 가능한지 확인할 수 있다")
    void canIssue() {
        // given
        Coupon coupon = Coupon.createFixed(
            "테스트 쿠폰",
            5000L,
            100,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            0L
        );

        // when & then
        assertThat(coupon.canIssue()).isTrue();

        for (int i = 0; i < 100; i++) {
            coupon.issue();
        }

        assertThat(coupon.canIssue()).isFalse();
    }

    @Test
    @DisplayName("유효기간이 지난 쿠폰은 발급할 수 없다")
    void issueCoupon_Expired() {
        // given
        Coupon coupon = Coupon.createFixed(
            "만료된 쿠폰",
            5000L,
            100,
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now().minusDays(1),
            0L
        );

        // when & then
        assertThrows(CustomException.class, coupon::issue);
    }

    @Test
    @DisplayName("비활성 상태의 쿠폰은 발급할 수 없다")
    void issueCoupon_Inactive() {
        // given
        Coupon coupon = Coupon.createFixed(
            "비활성 쿠폰",
            5000L,
            100,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            0L
        );
        coupon.deactivate();

        // when & then
        assertThrows(CustomException.class, coupon::issue);
    }

    @Test
    @DisplayName("할인 금액을 계산할 수 있다 - 정액 할인")
    void calculateDiscountAmount_Fixed() {
        // given
        Coupon coupon = Coupon.createFixed(
            "5000원 할인",
            5000L,
            100,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            10000L
        );

        // when
        Money discountAmount = coupon.calculateDiscountAmount(Money.of(20000L));

        // then
        assertThat(discountAmount).isEqualTo(Money.of(5000L));
    }

    @Test
    @DisplayName("할인 금액을 계산할 수 있다 - 정률 할인")
    void calculateDiscountAmount_Percentage() {
        // given
        Coupon coupon = Coupon.createPercentage(
            "10% 할인",
            10.0,
            100,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            20000L
        );

        // when
        Money discountAmount = coupon.calculateDiscountAmount(Money.of(50000L));

        // then
        assertThat(discountAmount).isEqualTo(Money.of(5000L)); // 50000 * 0.1
    }

    @Test
    @DisplayName("최소 주문 금액 미만이면 사용할 수 없다")
    void calculateDiscountAmount_BelowMinOrderAmount() {
        // given
        Coupon coupon = Coupon.createFixed(
            "5000원 할인",
            5000L,
            100,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            10000L
        );

        // when & then
        assertThrows(CustomException.class, () ->
            coupon.calculateDiscountAmount(Money.of(8000L))
        );
    }

    @Test
    @DisplayName("유효기간 내에 있는지 확인할 수 있다")
    void isValid() {
        // given
        Coupon validCoupon = Coupon.createFixed(
            "유효한 쿠폰",
            5000L,
            100,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(30),
            0L
        );

        Coupon expiredCoupon = Coupon.createFixed(
            "만료된 쿠폰",
            5000L,
            100,
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now().minusDays(1),
            0L
        );

        // when & then
        assertThat(validCoupon.isValid()).isTrue();
        assertThat(expiredCoupon.isValid()).isFalse();
    }

    @Test
    @DisplayName("쿠폰 상태를 변경할 수 있다")
    void changeStatus() {
        // given
        Coupon coupon = Coupon.createFixed(
            "테스트 쿠폰",
            5000L,
            100,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            0L
        );

        // when
        coupon.deactivate();

        // then
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.INACTIVE);
    }
}
