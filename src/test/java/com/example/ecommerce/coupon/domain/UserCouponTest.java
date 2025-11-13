package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.status.UserCouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserCoupon 도메인 테스트")
class UserCouponTest {

    private UserCoupon createUserCoupon(Long userId, Long couponId, LocalDateTime expiresAt) {
        return UserCoupon.builder()
            .id(1L)
            .userId(userId)
            .couponId(couponId)
            .expiresAt(expiresAt)
            .build();
    }

    private UserCoupon createValidUserCoupon() {
        return createUserCoupon(1L, 1L, LocalDateTime.now().plusDays(30));
    }

    private UserCoupon createExpiredUserCoupon() {
        return createUserCoupon(1L, 1L, LocalDateTime.now().minusDays(1));
    }

    private void assertThrowsCustomException(ErrorCode expectedErrorCode, Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(expectedErrorCode);
    }

    @Nested
    @DisplayName("UserCoupon 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("UserCoupon을 정상적으로 생성할 수 있다")
        void create_Success() {
            // given
            Long userId = 1L;
            Long couponId = 100L;
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

            // when
            UserCoupon userCoupon = UserCoupon.builder()
                .id(1L)
                .userId(userId)
                .couponId(couponId)
                .expiresAt(expiresAt)
                .build();

            // then
            assertThat(userCoupon.getUserId()).isEqualTo(userId);
            assertThat(userCoupon.getCouponId()).isEqualTo(couponId);
            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.UNUSED);
            assertThat(userCoupon.getUsedAt()).isNull();
            assertThat(userCoupon.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(userCoupon.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("생성 시 초기 상태는 UNUSED이다")
        void create_InitialStatusIsUnused() {
            // given & when
            UserCoupon userCoupon = createValidUserCoupon();

            // then
            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.UNUSED);
        }

        @Test
        @DisplayName("생성 시 usedAt은 null이다")
        void create_InitialUsedAtIsNull() {
            // given & when
            UserCoupon userCoupon = createValidUserCoupon();

            // then
            assertThat(userCoupon.getUsedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 테스트")
    class UseTest {

        @Test
        @DisplayName("미사용 쿠폰을 정상적으로 사용할 수 있다")
        void use_Success() {
            // given
            UserCoupon userCoupon = createValidUserCoupon();

            // when
            userCoupon.use();

            // then
            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
            assertThat(userCoupon.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 사용된 쿠폰은 다시 사용할 수 없다")
        void use_AlreadyUsed() {
            // given
            UserCoupon userCoupon = createValidUserCoupon();
            userCoupon.use();

            // when & then
            assertThrowsCustomException(
                ErrorCode.COUPON_ALREADY_USED,
                userCoupon::use
            );
        }

        @Test
        @DisplayName("만료된 쿠폰은 사용할 수 없다")
        void use_Expired() {
            // given
            UserCoupon userCoupon = createExpiredUserCoupon();

            // when & then
            assertThrowsCustomException(
                ErrorCode.COUPON_EXPIRED,
                userCoupon::use
            );
        }
    }

    @Nested
    @DisplayName("사용 가능 여부 확인 테스트")
    class CanUseTest {

        @Test
        @DisplayName("미사용이고 만료되지 않은 쿠폰은 사용 가능하다")
        void canUse_UnusedAndNotExpired() {
            // given
            UserCoupon userCoupon = createValidUserCoupon();

            // when & then
            assertThat(userCoupon.canUse()).isTrue();
        }

        @Test
        @DisplayName("이미 사용된 쿠폰은 사용 불가능하다")
        void canUse_AlreadyUsed() {
            // given
            UserCoupon userCoupon = createValidUserCoupon();
            userCoupon.use();

            // when & then
            assertThat(userCoupon.canUse()).isFalse();
        }

        @Test
        @DisplayName("만료된 쿠폰은 사용 불가능하다")
        void canUse_Expired() {
            // given
            UserCoupon userCoupon = createExpiredUserCoupon();

            // when & then
            assertThat(userCoupon.canUse()).isFalse();
        }
    }

    @Nested
    @DisplayName("사용 여부 확인 테스트")
    class IsUsedTest {

        @Test
        @DisplayName("사용된 쿠폰은 isUsed()가 true를 반환한다")
        void isUsed_Used() {
            // given
            UserCoupon userCoupon = createValidUserCoupon();
            userCoupon.use();

            // when & then
            assertThat(userCoupon.isUsed()).isTrue();
        }

        @Test
        @DisplayName("미사용 쿠폰은 isUsed()가 false를 반환한다")
        void isUsed_Unused() {
            // given
            UserCoupon userCoupon = createValidUserCoupon();

            // when & then
            assertThat(userCoupon.isUsed()).isFalse();
        }
    }

    @Nested
    @DisplayName("만료 여부 확인 테스트")
    class IsExpiredTest {

        @Test
        @DisplayName("만료 시간이 지난 쿠폰은 isExpired()가 true를 반환한다")
        void isExpired_AfterExpiresAt() {
            // given
            UserCoupon userCoupon = createExpiredUserCoupon();

            // when & then
            assertThat(userCoupon.isExpired()).isTrue();
        }

        @Test
        @DisplayName("만료 시간이 지나지 않은 쿠폰은 isExpired()가 false를 반환한다")
        void isExpired_BeforeExpiresAt() {
            // given
            UserCoupon userCoupon = createValidUserCoupon();

            // when & then
            assertThat(userCoupon.isExpired()).isFalse();
        }

        @Test
        @DisplayName("상태가 EXPIRED인 쿠폰은 isExpired()가 true를 반환한다")
        void isExpired_StatusExpired() {
            // given
            UserCoupon userCoupon = createValidUserCoupon();
            // Reflection을 사용하여 status를 EXPIRED로 변경
            try {
                var statusField = UserCoupon.class.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(userCoupon, UserCouponStatus.EXPIRED);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // when & then
            assertThat(userCoupon.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("쿠폰 발급부터 사용까지의 정상 플로우")
        void couponLifeCycle_NormalFlow() {
            // given - 쿠폰 발급
            UserCoupon userCoupon = createValidUserCoupon();

            // then - 발급 직후 상태 확인
            assertThat(userCoupon.canUse()).isTrue();
            assertThat(userCoupon.isUsed()).isFalse();
            assertThat(userCoupon.isExpired()).isFalse();

            // when - 쿠폰 사용
            userCoupon.use();

            // then - 사용 후 상태 확인
            assertThat(userCoupon.canUse()).isFalse();
            assertThat(userCoupon.isUsed()).isTrue();
            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
            assertThat(userCoupon.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("만료된 쿠폰의 상태 확인")
        void expiredCoupon_StatusCheck() {
            // given
            UserCoupon userCoupon = createExpiredUserCoupon();

            // when & then
            assertThat(userCoupon.canUse()).isFalse();
            assertThat(userCoupon.isExpired()).isTrue();
            assertThat(userCoupon.isUsed()).isFalse();
        }
    }
}
