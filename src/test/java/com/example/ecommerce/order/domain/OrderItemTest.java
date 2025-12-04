package com.example.ecommerce.order.domain;

import com.example.ecommerce.order.domain.status.OrderItemStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("OrderItem 도메인 테스트")
class OrderItemTest {

    private OrderItem createDefaultOrderItem() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return com.example.ecommerce.order.domain.OrderItem.builder()
            .productId(1L)
            .productName("테스트 상품")
            .quantity(2)
            .unitPrice(10000L)
            .subtotal(20000L)
            .status(com.example.ecommerce.order.domain.status.OrderItemStatus.ORDERED)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private void assertOrderItemFields(OrderItem orderItem, Long expectedProductId,
        String expectedProductName, Integer expectedQuantity, Long expectedUnitPrice,
        Long expectedSubtotal) {
        assertAll(
            () -> assertThat(orderItem.getProductId()).isEqualTo(expectedProductId),
            () -> assertThat(orderItem.getProductName()).isEqualTo(expectedProductName),
            () -> assertThat(orderItem.getQuantity()).isEqualTo(expectedQuantity),
            () -> assertThat(orderItem.getUnitPrice()).isEqualTo(expectedUnitPrice),
            () -> assertThat(orderItem.getSubtotal()).isEqualTo(expectedSubtotal),
            () -> assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.ORDERED),
            () -> assertThat(orderItem.getCreatedAt()).isNotNull(),
            () -> assertThat(orderItem.getUpdatedAt()).isNotNull()
        );
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 값으로 OrderItem을 생성한다")
        void createOrderItemWithValidValues() {
            // given
            Long productId = 1L;
            String productName = "노트북";
            Integer quantity = 2;
            Long unitPrice = 1500000L;

            // when
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            OrderItem orderItem = OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(quantity * unitPrice)
                .status(com.example.ecommerce.order.domain.status.OrderItemStatus.ORDERED)
                .createdAt(now)
                .updatedAt(now)
                .build();

            // then
            assertOrderItemFields(orderItem, productId, productName, quantity, unitPrice,
                3000000L);
        }

        @Test
        @DisplayName("생성 시 초기 상태는 ORDERED이다")
        void createWithInitialStatusOrdered() {
            // given & when
            OrderItem orderItem = createDefaultOrderItem();

            // then
            assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.ORDERED);
        }
    }

    @Nested
    @DisplayName("소계 계산 테스트")
    class SubtotalTest {

        @ParameterizedTest
        @CsvSource({
            "1, 1000, 1000",
            "1, 10000, 10000",
            "3, 5000, 15000",
            "10, 2000, 20000",
            "100, 500, 50000"
        })
        @DisplayName("다양한 수량과 단가로 소계를 정확히 계산한다")
        void calculateSubtotalWithVariousInputs(Integer quantity, Long unitPrice,
            Long expectedSubtotal) {
            // given
            Long productId = 1L;
            String productName = "상품";

            // when
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            OrderItem orderItem = OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(quantity * unitPrice)
                .status(com.example.ecommerce.order.domain.status.OrderItemStatus.ORDERED)
                .createdAt(now)
                .updatedAt(now)
                .build();

            // then
            assertThat(orderItem.getSubtotal()).isEqualTo(expectedSubtotal);
        }
    }
}
