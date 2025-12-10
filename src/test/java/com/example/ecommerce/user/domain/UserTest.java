package com.example.ecommerce.user.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.user.domain.status.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("User 도메인 테스트")
class UserTest {

    // 헬퍼 메서드
    private User createUser(String name, String email, Long balance) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return User.builder()
            .name(name)
            .email(email)
            .balance(balance)
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private User createDefaultUser() {
        return createUser("테스트유저", "test@example.com", 10000L);
    }

    private void assertUserFields(User user, String expectedName, String expectedEmail, Long expectedBalance) {
        assertAll(
            () -> assertThat(user.getName()).isEqualTo(expectedName),
            () -> assertThat(user.getEmail()).isEqualTo(expectedEmail),
            () -> assertThat(user.getBalance()).isEqualTo(expectedBalance),
            () -> assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE),
            () -> assertThat(user.getDeletedAt()).isNull(),
            () -> assertThat(user.getCreatedAt()).isNotNull(),
            () -> assertThat(user.getUpdatedAt()).isNotNull()
        );
    }

    private void assertThrowsCustomException(ErrorCode expectedErrorCode, Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 값으로 유저를 생성한다")
        void createUserWithValidValues() {
            // given
            String name = "홍길동";
            String email = "hong@example.com";
            Long balance = 50000L;

            // when
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            User user = User.builder()
                .name(name)
                .email(email)
                .balance(balance)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

            // then
            assertUserFields(user, name, email, balance);
        }

        @Test
        @DisplayName("잔액을 명시하지 않으면 0원으로 유저를 생성한다")
        void createUserWithDefaultBalance() {
            // given
            String name = "홍길동";
            String email = "hong@example.com";

            // when
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            User user = User.builder()
                .name(name)
                .email(email)
                .balance(0L)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

            // then
            assertUserFields(user, name, email, 0L);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 100L, 1000000L})
        @DisplayName("0 이상의 잔액으로 유저를 생성한다")
        void createUserWithVariousValidBalances(Long balance) {
            // given
            String name = "홍길동";
            String email = "hong@example.com";

            // when
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            User user = User.builder()
                .name(name)
                .email(email)
                .balance(balance)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

            // then
            assertThat(user.getBalance()).isEqualTo(balance);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, -100L, -1000L})
        @DisplayName("음수 잔액으로 유저를 생성하면 예외가 발생한다")
        void createUserWithNegativeBalance(Long balance) {
            // given
            String name = "홍길동";
            String email = "hong@example.com";

            // when & then
            assertThrowsCustomException(
                ErrorCode.USER_INVALID_BALANCE,
                () -> User.create(name, email, balance)
            );
        }

        @Test
        @DisplayName("null 잔액으로 유저를 생성하면 예외가 발생한다")
        void createUserWithNullBalance() {
            // given
            String name = "홍길동";
            String email = "hong@example.com";

            // when & then
            assertThrowsCustomException(
                ErrorCode.USER_INVALID_BALANCE,
                () -> User.create(name, email, null)
            );
        }

        @Test
        @DisplayName("생성된 유저의 상태는 ACTIVE이다")
        void createdUserStatusIsActive() {
            // given & when
            User user = createDefaultUser();

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("생성된 유저의 deletedAt은 null이다")
        void createdUserDeletedAtIsNull() {
            // given & when
            User user = createDefaultUser();

            // then
            assertThat(user.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("잔액 충전 테스트")
    class ChargeBalanceTest {

        @ParameterizedTest
        @CsvSource({
            "10000, 5000, 15000",
            "0, 10000, 10000",
            "50000, 1, 50001",
            "100000, 100000, 200000"
        })
        @DisplayName("유효한 금액으로 잔액을 충전한다")
        void chargeBalanceWithValidAmount(Long initialBalance, Long chargeAmount, Long expectedBalance) {
            // given
            User user = createUser("홍길동", "hong@example.com", initialBalance);

            // when
            user.chargeBalance(chargeAmount);

            // then
            assertThat(user.getBalance()).isEqualTo(expectedBalance);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -1000L})
        @DisplayName("0 이하의 금액으로 충전하면 예외가 발생한다")
        void chargeBalanceWithInvalidAmount(Long chargeAmount) {
            // given
            User user = createDefaultUser();

            // when & then
            assertThrowsCustomException(
                ErrorCode.USER_INVALID_CHARGE_AMOUNT,
                () -> user.chargeBalance(chargeAmount)
            );
        }

        @Test
        @DisplayName("null 금액으로 충전하면 예외가 발생한다")
        void chargeBalanceWithNullAmount() {
            // given
            User user = createDefaultUser();

            // when & then
            assertThrowsCustomException(
                ErrorCode.USER_INVALID_CHARGE_AMOUNT,
                () -> user.chargeBalance(null)
            );
        }

    }

    @Nested
    @DisplayName("잔액 차감 테스트")
    class DeductBalanceTest {

        @ParameterizedTest
        @CsvSource({
            "10000, 5000, 5000",
            "10000, 10000, 0",
            "50000, 1, 49999",
            "100000, 50000, 50000"
        })
        @DisplayName("충분한 잔액이 있으면 차감한다")
        void deductBalanceWithSufficientBalance(Long initialBalance, Long deductAmount, Long expectedBalance) {
            // given
            User user = createUser("홍길동", "hong@example.com", initialBalance);

            // when
            user.deductBalance(deductAmount);

            // then
            assertThat(user.getBalance()).isEqualTo(expectedBalance);
        }

        @ParameterizedTest
        @CsvSource({
            "10000, 10001",
            "5000, 10000",
            "0, 1",
            "1000, 5000"
        })
        @DisplayName("잔액이 부족하면 예외가 발생한다")
        void deductBalanceWithInsufficientBalance(Long initialBalance, Long deductAmount) {
            // given
            User user = createUser("홍길동", "hong@example.com", initialBalance);

            // when & then
            assertThrowsCustomException(
                ErrorCode.USER_INSUFFICIENT_BALANCE,
                () -> user.deductBalance(deductAmount)
            );
        }

        @Test
        @DisplayName("잔액이 부족하면 차감되지 않는다")
        void balanceIsNotDeductedWhenInsufficient() {
            // given
            User user = createUser("홍길동", "hong@example.com", 10000L);
            Long originalBalance = user.getBalance();

            // when & then
            assertThatThrownBy(() -> user.deductBalance(20000L))
                .isInstanceOf(CustomException.class);
            assertThat(user.getBalance()).isEqualTo(originalBalance);
        }
    }

    @Nested
    @DisplayName("잔액 확인 테스트")
    class HasEnoughBalanceTest {

        @ParameterizedTest
        @CsvSource({
            "10000, 5000, true",
            "10000, 10000, true",
            "10000, 10001, false",
            "10000, 20000, false",
            "0, 0, true",
            "0, 1, false"
        })
        @DisplayName("필요 금액에 대한 잔액 충분 여부를 확인한다")
        void checkIfBalanceIsSufficient(Long balance, Long requiredAmount, boolean expected) {
            // given
            User user = createUser("홍길동", "hong@example.com", balance);

            // when
            boolean result = user.hasEnoughBalance(requiredAmount);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("잔액이 필요 금액과 정확히 같으면 true를 반환한다")
        void returnTrueWhenBalanceEqualsRequiredAmount() {
            // given
            User user = createUser("홍길동", "hong@example.com", 10000L);

            // when
            boolean result = user.hasEnoughBalance(10000L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("잔액이 필요 금액보다 많으면 true를 반환한다")
        void returnTrueWhenBalanceIsGreaterThanRequiredAmount() {
            // given
            User user = createUser("홍길동", "hong@example.com", 10000L);

            // when
            boolean result = user.hasEnoughBalance(5000L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("잔액이 필요 금액보다 적으면 false를 반환한다")
        void returnFalseWhenBalanceIsLessThanRequiredAmount() {
            // given
            User user = createUser("홍길동", "hong@example.com", 5000L);

            // when
            boolean result = user.hasEnoughBalance(10000L);

            // then
            assertThat(result).isFalse();
        }
    }
}
