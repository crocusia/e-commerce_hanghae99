package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.product.domain.status.StockStatus;
import com.example.ecommerce.product.domain.vo.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("ProductStock 도메인 테스트")
class ProductStockTest {

    private static final int LOW_STOCK_THRESHOLD = 10;

    // 헬퍼 메서드
    private ProductStock create(Long productId, int stock) {
        return ProductStock.builder()
            .id(productId)
            .stock(stock)
            .build();
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적으로 상품 재고를 생성한다")
        void createProductStock() {
            // given
            Long productId = 1L;
            int initialStock = 100;

            // when
            ProductStock productStock = create(productId, initialStock);

            // then
            assertAll(
                () -> assertThat(productStock.getProductId()).isEqualTo(productId),
                () -> assertThat(productStock.getCurrentStock()).isEqualTo(Stock.of(initialStock)),
                () -> assertThat(productStock.getReservedStock()).isZero(),
                () -> assertThat(productStock.getUpdatedAt()).isNotNull()
            );
        }
    }

    @Nested
    @DisplayName("재고 감소 테스트")
    class DecreaseStockTest {

        @ParameterizedTest
        @CsvSource({
            "100, 10, 90",
            "100, 50, 50",
            "100, 100, 0"
        })
        @DisplayName("재고를 정상적으로 감소시킨다")
        void decreaseStock(int initialStock, int decreaseAmount, int expectedStock) {
            // given
            ProductStock productStock = create(1L, initialStock);

            // when
            productStock.decreaseStock(decreaseAmount);

            // then
            assertThat(productStock.getCurrentStock()).isEqualTo(Stock.of(expectedStock));
        }

        @Test
        @DisplayName("재고 감소 시 updatedAt이 갱신된다")
        void decreaseStockUpdatesTimestamp() {
            // given
            ProductStock productStock = create(1L, 100);
            var beforeUpdate = productStock.getUpdatedAt();

            // when
            productStock.decreaseStock(10);

            // then
            assertThat(productStock.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        }

        @Test
        @DisplayName("재고가 부족하면 예외가 발생한다")
        void decreaseStockWithInsufficientStock() {
            // given
            ProductStock productStock = create(1L, 10);

            // when & then
            assertThatThrownBy(() -> productStock.decreaseStock(20))
                .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("재고 증가 테스트")
    class IncreaseStockTest {

        @ParameterizedTest
        @CsvSource({
            "100, 10, 110",
            "0, 50, 50",
            "50, 100, 150"
        })
        @DisplayName("재고를 정상적으로 증가시킨다")
        void increaseStock(int initialStock, int increaseAmount, int expectedStock) {
            // given
            ProductStock productStock = create(1L, initialStock);

            // when
            productStock.increaseStock(increaseAmount);

            // then
            assertThat(productStock.getCurrentStock()).isEqualTo(Stock.of(expectedStock));
        }

        @Test
        @DisplayName("재고 증가 시 updatedAt이 갱신된다")
        void increaseStockUpdatesTimestamp() {
            // given
            ProductStock productStock = create(1L, 100);
            var beforeUpdate = productStock.getUpdatedAt();

            // when
            productStock.increaseStock(10);

            // then
            assertThat(productStock.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        }
    }

    @Nested
    @DisplayName("예약 재고 관리 테스트")
    class ReservedStockTest {

        @ParameterizedTest
        @CsvSource({
            "0, 10, 10",
            "10, 20, 30",
            "50, 5, 55"
        })
        @DisplayName("예약 재고를 정상적으로 증가시킨다")
        void increaseReservedStock(int initialReserved, int increaseAmount, int expectedReserved) {
            // given
            ProductStock productStock = create(1L, 100);
            for (int i = 0; i < initialReserved; i++) {
                productStock.increaseReservedStock(1);
            }

            // when
            productStock.increaseReservedStock(increaseAmount);

            // then
            assertThat(productStock.getReservedStock()).isEqualTo(expectedReserved);
        }

        @ParameterizedTest
        @CsvSource({
            "30, 10, 20",
            "50, 50, 0",
            "100, 25, 75"
        })
        @DisplayName("예약 재고를 정상적으로 감소시킨다")
        void decreaseReservedStock(int initialReserved, int decreaseAmount, int expectedReserved) {
            // given
            ProductStock productStock = create(1L, 100);
            productStock.increaseReservedStock(initialReserved);

            // when
            productStock.decreaseReservedStock(decreaseAmount);

            // then
            assertThat(productStock.getReservedStock()).isEqualTo(expectedReserved);
        }

        @Test
        @DisplayName("예약 재고 변경 시 updatedAt이 갱신된다")
        void reservedStockChangeUpdatesTimestamp() {
            // given
            ProductStock productStock = create(1L, 100);
            var beforeUpdate = productStock.getUpdatedAt();

            // when
            productStock.increaseReservedStock(10);

            // then
            assertThat(productStock.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        }
    }

    @Nested
    @DisplayName("예약 가능 여부 확인 테스트")
    class HasEnoughStockToReservationTest {

        @ParameterizedTest
        @CsvSource({
            "100, 0, 50, true",
            "100, 50, 50, true",
            "100, 50, 49, true",
            "100, 50, 51, false",
            "100, 100, 1, false",
            "50, 25, 25, true",
            "50, 25, 26, false"
        })
        @DisplayName("예약 가능 여부를 정확히 판단한다")
        void hasEnoughStockToReservation(int currentStock, int reservedStock, int requestQuantity, boolean expected) {
            // given
            ProductStock productStock = create(1L, currentStock);
            productStock.increaseReservedStock(reservedStock);

            // when
            boolean result = productStock.hasEnoughStockToReservation(requestQuantity);

            // then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("재고 상태 조회 테스트")
    class GetStockStatusTest {

        @ParameterizedTest
        @CsvSource({
            "0, OUT_OF_STOCK",
            "1, LOW_STOCK",
            "5, LOW_STOCK",
            "10, LOW_STOCK",
            "11, AVAILABLE",
            "100, AVAILABLE"
        })
        @DisplayName("재고량에 따라 올바른 재고 상태를 반환한다")
        void getStockStatus(int stock, StockStatus expectedStatus) {
            // given
            ProductStock productStock = create(1L, stock);

            // when
            StockStatus status = productStock.getStockStatus(LOW_STOCK_THRESHOLD);

            // then
            assertThat(status).isEqualTo(expectedStatus);
        }
    }
}
