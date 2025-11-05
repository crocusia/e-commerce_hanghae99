package com.example.ecommerce.domain.product;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.product.domain.Stock;
import com.example.ecommerce.product.domain.StockStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Stock VO 테스트")
class StockTest {

    private Stock stock100;
    private Stock stock50;
    private Stock stock10;

    @BeforeEach
    void setUp() {
        stock100 = Stock.of(100);
        stock50 = Stock.of(50);
        stock10 = Stock.of(10);
    }

    @Test
    @DisplayName("Stock 객체를 생성할 수 있다")
    void createStock() {
        // given
        int quantity = 100;

        // when
        Stock stock = Stock.of(quantity);

        // then
        assertThat(stock).isNotNull();
        assertThat(stock.getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("빈 재고로 Stock 객체를 생성할 수 있다")
    void createEmptyStock() {
        // when
        Stock stock = Stock.empty();

        // then
        assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("음수 재고로 Stock 객체를 생성하면 예외가 발생한다")
    void createStock_NegativeQuantity() {
        // when & then
        assertThrows(CustomException.class, () ->
            Stock.of(-10)
        );
    }

    @Test
    @DisplayName("재고를 차감할 수 있다")
    void decreaseStock() {
        // when
        Stock result = stock100.decrease(30);

        // then
        assertThat(result.getQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
    void decreaseStock_ExceedsQuantity() {
        // when & then
        assertThrows(CustomException.class, () ->
            stock10.decrease(20)
        );
    }

    @Test
    @DisplayName("0으로 차감하면 예외가 발생한다")
    void decreaseStock_WithZero() {
        // when & then
        assertThrows(CustomException.class, () ->
            stock100.decrease(0)
        );
    }

    @Test
    @DisplayName("음수로 차감하면 예외가 발생한다")
    void decreaseStock_WithNegative() {
        // when & then
        assertThrows(CustomException.class, () ->
            stock100.decrease(-10)
        );
    }

    @Test
    @DisplayName("재고를 증가시킬 수 있다")
    void increaseStock() {
        // when
        Stock result = stock50.increase(30);

        // then
        assertThat(result.getQuantity()).isEqualTo(80);
    }

    @Test
    @DisplayName("0으로 증가하면 예외가 발생한다")
    void increaseStock_WithZero() {
        // when & then
        assertThrows(CustomException.class, () ->
            stock50.increase(0)
        );
    }

    @Test
    @DisplayName("음수로 증가하면 예외가 발생한다")
    void increaseStock_WithNegative() {
        // when & then
        assertThrows(CustomException.class, () ->
            stock50.increase(-10)
        );
    }

    @Test
    @DisplayName("요청 수량보다 재고가 많을 때 충분함을 반환한다")
    void hasEnough_WhenStockIsGreater() {
        // when & then
        assertThat(stock100.hasEnough(50)).isTrue();
    }

    @Test
    @DisplayName("요청 수량과 재고가 같을 때 충분함을 반환한다")
    void hasEnough_WhenStockIsEqual() {
        // when & then
        assertThat(stock100.hasEnough(100)).isTrue();
    }

    @Test
    @DisplayName("요청 수량보다 재고가 적을 때 부족함을 반환한다")
    void hasEnough_WhenStockIsLess() {
        // when & then
        assertThat(stock100.hasEnough(101)).isFalse();
    }

    @Test
    @DisplayName("재고 상태가 품절일 때 OUT_OF_STOCK을 반환한다")
    void getStatus_OutOfStock() {
        // given
        Stock stock = Stock.of(0);

        // when
        StockStatus status = stock.getStatus();

        // then
        assertThat(status).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("재고가 1일 때 LOW_STOCK을 반환한다")
    void getStatus_LowStock_WithMinimum() {
        // given
        Stock stock = Stock.of(1);

        // when
        StockStatus status = stock.getStatus();

        // then
        assertThat(status).isEqualTo(StockStatus.LOW_STOCK);
    }

    @Test
    @DisplayName("재고가 10일 때 LOW_STOCK을 반환한다")
    void getStatus_LowStock_WithThreshold() {
        // when
        StockStatus status = stock10.getStatus();

        // then
        assertThat(status).isEqualTo(StockStatus.LOW_STOCK);
    }

    @Test
    @DisplayName("재고가 11일 때 AVAILABLE을 반환한다")
    void getStatus_Available() {
        // given
        Stock stock = Stock.of(11);

        // when
        StockStatus status = stock.getStatus();

        // then
        assertThat(status).isEqualTo(StockStatus.AVAILABLE);
    }

    @Test
    @DisplayName("같은 수량의 Stock 객체는 동등하다")
    void equalsStock_SameQuantity() {
        // when & then
        assertThat(stock100).isEqualTo(Stock.of(100));
    }

    @Test
    @DisplayName("다른 수량의 Stock 객체는 동등하지 않다")
    void equalsStock_DifferentQuantity() {
        // when & then
        assertThat(stock100).isNotEqualTo(stock50);
    }

    @Test
    @DisplayName("Stock 객체를 문자열로 변환할 수 있다")
    void toStringStock() {
        // when
        String result = stock100.toString();

        // then
        assertThat(result).isEqualTo("100");
    }

    @Test
    @DisplayName("차감 후 원본 Stock 객체는 변경되지 않는다")
    void stockIsImmutable_AfterDecrease() {
        // when
        Stock decreased = stock100.decrease(30);

        // then
        assertThat(stock100.getQuantity()).isEqualTo(100);
        assertThat(decreased.getQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("증가 후 원본 Stock 객체는 변경되지 않는다")
    void stockIsImmutable_AfterIncrease() {
        // when
        Stock increased = stock100.increase(20);

        // then
        assertThat(stock100.getQuantity()).isEqualTo(100);
        assertThat(increased.getQuantity()).isEqualTo(120);
    }
}
