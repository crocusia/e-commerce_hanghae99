package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.status.StockStatus;
import com.example.ecommerce.product.domain.vo.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Stock VO 테스트")
class StockTest {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private Stock createStock(int quantity) {
        return Stock.of(quantity);
    }

    private void assertStockEquals(int expected, Stock actual) {
        assertThat(actual.getQuantity()).isEqualTo(expected);
    }

    private void assertThrowsInvalidInputValue(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    private void assertThrowsOutOfStock(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_OUT_OF_STOCK);
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적인 수량으로 Stock 객체를 생성할 수 있다")
        void createWithValidQuantity() {
            // when
            Stock stock = createStock(100);

            // then
            assertStockEquals(100, stock);
        }

        @Test
        @DisplayName("0개로 Stock 객체를 생성할 수 있다")
        void createWithZero() {
            // when
            Stock stock = Stock.empty();

            // then
            assertStockEquals(0, stock);
        }

        @Test
        @DisplayName("음수 수량으로 Stock 객체를 생성하면 예외가 발생한다")
        void createWithNegativeQuantity() {
            // when & then
            assertThrowsInvalidInputValue(() -> createStock(-10));
        }
    }

    @Nested
    @DisplayName("재고 차감 테스트")
    class DecreaseTest {

        @ParameterizedTest
        @CsvSource({
                "100, 30, 70",
                "50, 50, 0",
                "10, 5, 5"
        })
        @DisplayName("재고를 정상적으로 차감할 수 있다")
        void decrease(int initialQuantity, int decreaseAmount, int expectedQuantity) {
            // given
            Stock stock = createStock(initialQuantity);

            // when
            Stock result = stock.decrease(decreaseAmount);

            // then
            assertStockEquals(expectedQuantity, result);
        }

        @Test
        @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
        void decreaseMoreThanStock() {
            // given
            Stock stock = createStock(10);

            // when & then
            assertThrowsOutOfStock(() -> stock.decrease(20));
        }
    }

    @Nested
    @DisplayName("재고 추가 테스트")
    class IncreaseTest {

        @ParameterizedTest
        @CsvSource({
                "100, 30, 130",
                "0, 50, 50",
                "10, 10, 20"
        })
        @DisplayName("재고를 정상적으로 추가할 수 있다")
        void increase(int initialQuantity, int increaseAmount, int expectedQuantity) {
            // given
            Stock stock = createStock(initialQuantity);

            // when
            Stock result = stock.increase(increaseAmount);

            // then
            assertStockEquals(expectedQuantity, result);
        }
    }

    @Nested
    @DisplayName("재고 확인 테스트")
    class HasEnoughTest {

        @Test
        @DisplayName("재고가 충분하면 true를 반환한다")
        void hasEnoughWhenStockIsSufficient() {
            // given
            Stock stock = createStock(100);

            // when & then
            assertThat(stock.hasEnough(50)).isTrue();
            assertThat(stock.hasEnough(100)).isTrue();
        }

        @Test
        @DisplayName("재고가 부족하면 false를 반환한다")
        void hasEnoughWhenStockIsInsufficient() {
            // given
            Stock stock = createStock(50);

            // when & then
            assertThat(stock.hasEnough(100)).isFalse();
        }

        @Test
        @DisplayName("0개를 요청하면 true를 반환한다")
        void hasEnoughWithZeroRequired() {
            // given
            Stock stock = createStock(0);

            // when & then
            assertThat(stock.hasEnough(0)).isTrue();
        }
    }

    @Nested
    @DisplayName("재고 상태 테스트")
    class StatusTest {

        @Test
        @DisplayName("재고가 0개 이하면 품절 상태를 반환한다")
        void outOfStock() {
            // given
            Stock stock = createStock(0);

            // when
            StockStatus status = stock.getStatus(LOW_STOCK_THRESHOLD);

            // then
            assertThat(status).isEqualTo(StockStatus.OUT_OF_STOCK);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 10})
        @DisplayName("재고가 1~10개면 품절임박 상태를 반환한다")
        void lowStock(int quantity) {
            // given
            Stock stock = createStock(quantity);

            // when
            StockStatus status = stock.getStatus(LOW_STOCK_THRESHOLD);

            // then
            assertThat(status).isEqualTo(StockStatus.LOW_STOCK);
        }

        @ParameterizedTest
        @ValueSource(ints = {11, 50, 100})
        @DisplayName("재고가 11개 이상이면 정상 상태를 반환한다")
        void available(int quantity) {
            // given
            Stock stock = createStock(quantity);

            // when
            StockStatus status = stock.getStatus(LOW_STOCK_THRESHOLD);

            // then
            assertThat(status).isEqualTo(StockStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("equals 테스트")
    class EqualsTest {

        @Test
        @DisplayName("같은 수량의 Stock 객체는 동등하다")
        void equalsWithSameQuantity() {
            // given
            Stock stock1 = createStock(100);
            Stock stock2 = createStock(100);

            // when & then
            assertThat(stock1).isEqualTo(stock2);
        }

        @Test
        @DisplayName("다른 수량의 Stock 객체는 동등하지 않다")
        void equalsWithDifferentQuantity() {
            // given
            Stock stock1 = createStock(100);
            Stock stock2 = createStock(50);

            // when & then
            assertThat(stock1).isNotEqualTo(stock2);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {
        @Test
        @DisplayName("toString()은 수량을 문자열로 반환한다")
        void toStringReturnsQuantity() {
            // given
            Stock stock = createStock(100);

            // when
            String result = stock.toString();

            // then
            assertThat(result).isEqualTo("100");
        }
    }
}