package com.example.ecommerce.domain.product;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.product.domain.Money;
import com.example.ecommerce.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Money VO 테스트")
class MoneyTest {

    private Money money1000;
    private Money money200;

    @BeforeEach
    void setUp() {
        money1000 = Money.of(1_000L);
        money200 = Money.of(200L);
    }


    @Test
    @DisplayName("Money 객체를 생성할 수 있다")
    void createMoney() {
        // given
        long amount = 10_000L;

        // when
        Money money = Money.of(amount);

        // then
        assertThat(money).isNotNull();
        assertThat(money.getAmount()).isEqualTo(amount);
    }

    @Test
    @DisplayName("0원으로 Money 객체를 생성할 수 있다")
    void createZeroMoney() {
        // when
        Money money = Money.zero();

        // then
        assertThat(money.getAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("음수 금액으로 Money 객체를 생성하면 예외가 발생한다")
    void createMoney_NegativeAmount() {
        // when & then
        assertThrows(CustomException.class, () ->
            Money.of(-1000L)
        );
    }

    @Test
    @DisplayName("두 Money 객체를 더할 수 있다")
    void addMoney() {
        // when
        Money result = money1000.add(money200);

        // then
        assertThat(result.getAmount()).isEqualTo(1_200L);
    }

    @Test
    @DisplayName("두 Money 객체를 뺄 수 있다")
    void subtractMoney() {
        // when
        Money result = money1000.subtract(money200);

        // then
        assertThat(result.getAmount()).isEqualTo(800L);
    }

    @Test
    @DisplayName("Money 객체에 수량을 곱할 수 있다")
    void multiplyMoney() {
        // given
        int quantity = 3;

        // when
        Money result = money1000.multiply(quantity);

        // then
        assertThat(result.getAmount()).isEqualTo(3_000L);
    }

    @Test
    @DisplayName("할인율을 적용할 수 있다")
    void applyDiscountRate() {
        // given
        double rate = 0.2;

        // when
        Money result = money1000.discountRate(rate);

        // then
        assertThat(result.getAmount()).isEqualTo(800L);
    }

    @Test
    @DisplayName("isGreaterThan: 현재 금액이 대상보다 크면 true를 반환한다")
    void isGreaterThan_Success() {
        // when & then
        assertTrue(money1000.isGreaterThan(money200));
    }

    @Test
    @DisplayName("isGreaterThan: 현재 금액이 대상과 같으면 false를 반환한다")
    void isGreaterThan_equal_Fail() {
        // given
        Money money = Money.of(1_000L);
        // when & then
        assertFalse(money.isGreaterThan(money1000));
    }
    @Test
    @DisplayName("isGreaterThan: 현재 금액이 대상보다 작으면 false를 반환한다")
    void isGreaterThan_small_Fail() {
        // when & then
        assertFalse(money200.isGreaterThan(money1000));
    }

    @Test
    @DisplayName("isGreaterThanOrEqual: 현재 금액이 대상보다 크거나 같으면 true를 반환한다")
    void isGreaterThanOrEqual_Success() {
        Money money = Money.of(1_000L);
        // when & then
        assertTrue(money.isGreaterThanOrEqual(money1000));
    }

    @Test
    @DisplayName("isGreaterThanOrEqual: 현재 금액이 대상보다 작으면 false를 반환한다")
    void isGreaterThanOrEqual_Fail() {
        // when & then
        assertFalse(money1000.isGreaterThanOrEqual(money200));
    }

    @Test
    @DisplayName("isLessThan: 현재 금액이 대상보다 작으면 true를 반환한다")
    void isLessThan_Success() {
        // when & then
        assertTrue(money200.isLessThan(money1000));
    }

    @Test
    @DisplayName("isLessThan: 현재 금액이 대상보다 크거나 같으면 false를 반환한다")
    void isLessThan_Fail() {
        // when & then
        assertFalse(money1000.isLessThan(money200));
    }



    @Test
    @DisplayName("같은 금액의 Money 객체는 동등하다")
    void equalsMoney() {
        // given
        Money money = Money.of(1_000L);

        // when & then
        assertThat(money1000).isEqualTo(money);
    }

    @Test
    @DisplayName("다른 금액의 Money 객체는 동등하지 않다")
    void equalsMoney_DifferentAmount() {
        assertThat(money1000).isNotEqualTo(money200);
    }

    @Test
    @DisplayName("Money 객체의 hashCode가 정상 동작한다")
    void hashCodeMoney() {
        // when & then
        assertThat(money1000.hashCode()).isEqualTo(money200.hashCode());
    }

    @Test
    @DisplayName("Money 객체를 문자열로 변환할 수 있다")
    void toStringMoney() {
        // when
        String result = money1000.toString();

        // then
        assertThat(result).isEqualTo("1000");
    }
}
