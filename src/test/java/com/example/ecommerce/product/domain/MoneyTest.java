package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money VO 테스트")
class MoneyTest {
    private Money createMoney(long amount) {
        return Money.of(amount);
    }

    private void assertMoneyEquals(long expected, Money actual) {
        assertThat(actual.getAmount()).isEqualTo(expected);
    }

    private void assertThrowsInvalidInputValue(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적인 금액으로 Money 객체를 생성할 수 있다")
        void createWithValidAmount() {
            // when
            Money money = createMoney(1000L);
            // then
            assertMoneyEquals(1000L, money);
        }

        @Test
        @DisplayName("0원으로 Money 객체를 생성할 수 있다")
        void createWithZero() {
            // when
            Money money = Money.zero();
            // then
            assertMoneyEquals(0L, money);
        }

        @Test
        @DisplayName("음수 금액으로 Money 객체를 생성하면 예외가 발생한다")
        void createWithNegativeAmount() {
            // when & then
            assertThrowsInvalidInputValue(() -> createMoney(-1000L));
        }
    }

    @Nested
    @DisplayName("연산 테스트")
    class OperationTest {

        @ParameterizedTest
        @CsvSource({
                "1000, 2000, 3000",
                "500, 500, 1000",
                "0, 1000, 1000"
        })
        @DisplayName("두 Money 객체를 더할 수 있다")
        void add(long amount1, long amount2, long expected) {
            // given
            Money money1 = createMoney(amount1);
            Money money2 = createMoney(amount2);

            // when
            Money result = money1.add(money2);

            // then
            assertMoneyEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
                "3000, 1000, 2000",
                "1000, 500, 500",
                "1000, 1000, 0"
        })
        @DisplayName("두 Money 객체를 뺄 수 있다")
        void subtract(long amount1, long amount2, long expected) {
            // given
            Money money1 = createMoney(amount1);
            Money money2 = createMoney(amount2);

            // when
            Money result = money1.subtract(money2);

            // then
            assertMoneyEquals(expected, result);
        }

        @Test
        @DisplayName("뺄셈 결과가 음수가 되면 예외가 발생한다")
        void subtractResultsInNegative() {
            // given
            Money money1 = createMoney(1000L);
            Money money2 = createMoney(2000L);

            // when & then
            assertThrowsInvalidInputValue(() -> money1.subtract(money2));
        }

        @ParameterizedTest
        @CsvSource({
                "1000, 3, 3000",
                "500, 2, 1000",
                "1000, 0, 0"
        })
        @DisplayName("Money 객체에 수량을 곱할 수 있다")
        void multiply(long amount, int quantity, long expected) {
            // given
            Money money = createMoney(amount);

            // when
            Money result = money.multiply(quantity);

            // then
            assertMoneyEquals(expected, result);
        }

        @Test
        @DisplayName("곱셈 결과가 음수가 되면 예외가 발생한다")
        void multiplyWithNegativeQuantity() {
            // given
            Money money = createMoney(1000L);

            // when & then
            assertThrowsInvalidInputValue(() -> money.multiply(-1));
        }

        @ParameterizedTest
        @CsvSource({
                "10000, 0.1, 9000",    // 10% 할인
                "10000, 0.2, 8000",    // 20% 할인
                "10000, 0.5, 5000",    // 50% 할인
                "10000, 0.0, 10000"    // 할인 없음
        })
        @DisplayName("할인율을 적용할 수 있다")
        void discountRate(long amount, double rate, long expected) {
            // given
            Money money = createMoney(amount);

            // when
            Money result = money.discountRate(rate);

            // then
            assertMoneyEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("비교 테스트")
    class ComparisonTest {

        @Test
        @DisplayName("더 큰 금액을 비교할 수 있다")
        void isGreaterThan() {
            // given
            Money money1 = createMoney(2000L);
            Money money2 = createMoney(1000L);
            Money money3 = createMoney(2000L);

            // when & then
            assertThat(money1.isGreaterThan(money2)).isTrue();
            assertThat(money2.isGreaterThan(money1)).isFalse();
            assertThat(money1.isGreaterThan(money3)).isFalse();
        }

        @Test
        @DisplayName("크거나 같은 금액을 비교할 수 있다")
        void isGreaterThanOrEqual() {
            // given
            Money money1 = createMoney(2000L);
            Money money2 = createMoney(1000L);
            Money money3 = createMoney(2000L);

            // when & then
            assertThat(money1.isGreaterThanOrEqual(money2)).isTrue();
            assertThat(money1.isGreaterThanOrEqual(money3)).isTrue();
            assertThat(money2.isGreaterThanOrEqual(money1)).isFalse();
        }

        @Test
        @DisplayName("더 작은 금액을 비교할 수 있다")
        void isLessThan() {
            // given
            Money money1 = createMoney(1000L);
            Money money2 = createMoney(2000L);
            Money money3 = createMoney(1000L);

            // when & then
            assertThat(money1.isLessThan(money2)).isTrue();
            assertThat(money2.isLessThan(money1)).isFalse();
            assertThat(money1.isLessThan(money3)).isFalse();
        }

        @Test
        @DisplayName("작거나 같은 금액을 비교할 수 있다")
        void isLessThanOrEqual() {
            // given
            Money money1 = createMoney(1000L);
            Money money2 = createMoney(2000L);
            Money money3 = createMoney(1000L);

            // when & then
            assertThat(money1.isLessThanOrEqual(money2)).isTrue();
            assertThat(money1.isLessThanOrEqual(money3)).isTrue();
            assertThat(money2.isLessThanOrEqual(money1)).isFalse();
        }
    }


    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsAndHashCodeTest {

        @Test
        @DisplayName("같은 금액의 Money 객체는 동등하다")
        void equalsWithSameAmount() {
            // given
            Money money1 = createMoney(1000L);
            Money money2 = createMoney(1000L);

            // when & then
            assertThat(money1).isEqualTo(money2);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        }

        @Test
        @DisplayName("다른 금액의 Money 객체는 동등하지 않다")
        void equalsWithDifferentAmount() {
            // given
            Money money1 = createMoney(1000L);
            Money money2 = createMoney(2000L);

            // when & then
            assertThat(money1).isNotEqualTo(money2);
        }

        @Test
        @DisplayName("같은 인스턴스는 동등하다")
        void equalsWithSameInstance() {
            // given
            Money money = createMoney(1000L);

            // when & then
            assertThat(money).isEqualTo(money);
        }

        @Test
        @DisplayName("null과 비교하면 동등하지 않다")
        void equalsWithNull() {
            // given
            Money money = createMoney(1000L);

            // when & then
            assertThat(money).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 비교하면 동등하지 않다")
        void equalsWithDifferentType() {
            // given
            Money money = createMoney(1000L);
            Object other = new Object();

            // when & then
            assertThat(money).isNotEqualTo(other);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString()은 금액을 문자열로 반환한다")
        void toStringReturnsAmount() {
            // given
            Money money = createMoney(1000L);

            // when
            String result = money.toString();

            // then
            assertThat(result).isEqualTo("1000");
        }

        @Test
        @DisplayName("0원의 toString()은 '0'을 반환한다")
        void toStringWithZero() {
            // given
            Money money = Money.zero();

            // when
            String result = money.toString();

            // then
            assertThat(result).isEqualTo("0");
        }
    }
}