package com.example.ecommerce.domain.coupon;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.domain.UserCouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("UserCoupon 도메인 테스트")
class UserCouponTest {

    @Test
    @DisplayName("사용자 쿠폰을 생성할 수 있다")
    void createUserCoupon() {
        // given
        Long userId = 1L;
        Coupon coupon = Coupon.createFixed(
            "5000원 할인 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );

        // when
        UserCoupon userCoupon = UserCoupon.create(userId, coupon);

        // then
        assertThat(userCoupon).isNotNull();
        assertThat(userCoupon.getUserId()).isEqualTo(userId);
        assertThat(userCoupon.getCoupon()).isEqualTo(coupon);
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.UNUSED);
        assertThat(userCoupon.isUsed()).isFalse();
    }

    @Test
    @DisplayName("사용자 쿠폰을 사용할 수 있다")
    void useCoupon() {
        // given
        Long userId = 1L;
        Coupon coupon = Coupon.createFixed(
            "5000원 할인 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        UserCoupon userCoupon = UserCoupon.create(userId, coupon);

        // when
        userCoupon.use();

        // then
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
        assertThat(userCoupon.isUsed()).isTrue();
        assertThat(userCoupon.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 사용된 쿠폰은 다시 사용할 수 없다")
    void useCoupon_AlreadyUsed() {
        // given
        Long userId = 1L;
        Coupon coupon = Coupon.createFixed(
            "5000원 할인 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        UserCoupon userCoupon = UserCoupon.create(userId, coupon);
        userCoupon.use();

        // when & then
        assertThrows(CustomException.class, userCoupon::use);
    }

    @Test
    @DisplayName("만료된 쿠폰은 사용할 수 없다")
    void useCoupon_Expired() {
        // given
        Long userId = 1L;
        Coupon coupon = Coupon.createFixed(
            "만료된 쿠폰",
            5000L,
            100,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(1),
            10000L
        );
        UserCoupon userCoupon = UserCoupon.create(userId, coupon);

        // when & then
        assertThrows(CustomException.class, userCoupon::use);
    }

    @Test
    @DisplayName("비활성화된 쿠폰은 사용할 수 없다")
    void useCoupon_Inactive() {
        // given
        Long userId = 1L;
        Coupon coupon = Coupon.createFixed(
            "비활성 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        coupon.deactivate();
        UserCoupon userCoupon = UserCoupon.create(userId, coupon);

        // when & then
        assertThrows(CustomException.class, userCoupon::use);
    }

    @Test
    @DisplayName("사용 가능한 쿠폰인지 확인할 수 있다")
    void canUse() {
        // given
        Long userId = 1L;
        Coupon validCoupon = Coupon.createFixed(
            "유효한 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        UserCoupon userCoupon = UserCoupon.create(userId, validCoupon);

        // when & then
        assertThat(userCoupon.canUse()).isTrue();

        // 사용 후에는 사용 불가
        userCoupon.use();
        assertThat(userCoupon.canUse()).isFalse();
    }

    @Test
    @DisplayName("만료된 쿠폰으로 표시할 수 있다")
    void expire() {
        // given
        Long userId = 1L;
        Coupon coupon = Coupon.createFixed(
            "테스트 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );
        UserCoupon userCoupon = UserCoupon.create(userId, coupon);

        // when
        userCoupon.expire();

        // then
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.EXPIRED);
        assertThat(userCoupon.canUse()).isFalse();
    }

    @Test
    @DisplayName("쿠폰이 만료되었는지 확인할 수 있다")
    void isExpired() {
        // given
        Long userId = 1L;
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
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );

        UserCoupon expiredUserCoupon = UserCoupon.create(userId, expiredCoupon);
        UserCoupon validUserCoupon = UserCoupon.create(userId, validCoupon);

        // when & then
        assertThat(expiredUserCoupon.isExpired()).isTrue();
        assertThat(validUserCoupon.isExpired()).isFalse();
    }

    @Test
    @DisplayName("null userId로 생성할 수 없다")
    void createUserCoupon_NullUserId() {
        // given
        Coupon coupon = Coupon.createFixed(
            "테스트 쿠폰",
            5000L,
            100,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            10000L
        );

        // when & then
        assertThrows(CustomException.class, () -> UserCoupon.create(null, coupon));
    }

    @Test
    @DisplayName("null Coupon으로 생성할 수 없다")
    void createUserCoupon_NullCoupon() {
        // when & then
        assertThrows(CustomException.class, () -> UserCoupon.create(1L, null));
    }
}
