package com.example.ecommerce.order.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.order.domain.status.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Order 도메인 테스트")
class OrderTest {

    // 헬퍼 메서드
    private OrderItem createOrderItem(Long productId, String productName, Integer quantity,
        Long unitPrice) {
        return OrderItem.create(productId, productName, quantity, unitPrice);
    }

    private List<OrderItem> createDefaultOrderItems() {
        return Arrays.asList(
            createOrderItem(1L, "상품1", 2, 10000L),
            createOrderItem(2L, "상품2", 1, 20000L)
        );
    }

    private Order createDefaultOrder() {
        return Order.create(1L, createDefaultOrderItems());
    }

    private void assertThrowsIllegalArgumentException(String expectedMessage, Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(expectedMessage);
    }

    private void assertThrowsCustomException(ErrorCode expectedErrorCode, Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(expectedErrorCode);
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 값으로 주문을 생성한다")
        void createOrderWithValidValues() {
            // given
            Long userId = 1L;
            List<OrderItem> orderItems = createDefaultOrderItems();

            // when
            Order order = Order.create(userId, orderItems);

            // then
            assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(userId),
                () -> assertThat(order.getOrderItems()).hasSize(2),
                () -> assertThat(order.getTotalAmount()).isEqualTo(40000L),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(0L),
                () -> assertThat(order.getFinalAmount()).isEqualTo(40000L),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(order.getUserCouponId()).isNull(),
                () -> assertThat(order.getCreatedAt()).isNotNull(),
                () -> assertThat(order.getUpdatedAt()).isNotNull()
            );
        }
    }

    @Nested
    @DisplayName("총 금액 계산 테스트")
    class TotalAmountTest {

        @ParameterizedTest
        @CsvSource({
            "1, 10000, 10000",
            "2, 10000, 20000",
            "5, 5000, 25000",
            "10, 1000, 10000"
        })
        @DisplayName("단일 상품의 총 금액을 정확히 계산한다")
        void calculateTotalAmountForSingleItem(Integer quantity, Long unitPrice,
            Long expectedTotal) {
            // given
            Long userId = 1L;
            List<OrderItem> orderItems = Arrays.asList(
                createOrderItem(1L, "상품", quantity, unitPrice)
            );

            // when
            Order order = Order.create(userId, orderItems);

            // then
            assertThat(order.getTotalAmount()).isEqualTo(expectedTotal);
        }

        @Test
        @DisplayName("여러 상품의 총 금액을 정확히 계산한다")
        void calculateTotalAmountForMultipleItems() {
            // given
            Long userId = 1L;
            List<OrderItem> orderItems = Arrays.asList(
                createOrderItem(1L, "상품1", 2, 10000L),  // 20000
                createOrderItem(2L, "상품2", 1, 15000L),  // 15000
                createOrderItem(3L, "상품3", 3, 5000L)    // 15000
            );

            // when
            Order order = Order.create(userId, orderItems);

            // then
            assertThat(order.getTotalAmount()).isEqualTo(50000L);
        }
    }

    @Nested
    @DisplayName("쿠폰 적용 테스트")
    class ApplyCouponTest {

        @Test
        @DisplayName("PENDING 상태에서 쿠폰과 할인을 정상적으로 적용한다")
        void applyCouponInPendingStatus() {
            // given
            Order order = createDefaultOrder();
            Long userCouponId = 1L;
            Long discountAmount = 5000L;

            // when
            order.applyCoupon(userCouponId, discountAmount);

            // then
            assertAll(
                () -> assertThat(order.getUserCouponId()).isEqualTo(userCouponId),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(discountAmount),
                () -> assertThat(order.getFinalAmount()).isEqualTo(35000L)
            );
        }

        @ParameterizedTest
        @CsvSource({
            "40000, 5000, 35000",
            "40000, 10000, 30000",
            "40000, 20000, 20000",
            "40000, 40000, 0",
            "40000, 0, 40000"
        })
        @DisplayName("다양한 할인 금액으로 쿠폰을 적용한다")
        void applyCouponWithVariousDiscounts(Long totalAmount, Long discountAmount,
            Long expectedFinalAmount) {
            // given
            Order order = createDefaultOrder();
            assertThat(order.getTotalAmount()).isEqualTo(totalAmount);

            // when
            order.applyCoupon(1L, discountAmount);

            // then
            assertAll(
                () -> assertThat(order.getUserCouponId()).isEqualTo(1L),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(discountAmount),
                () -> assertThat(order.getFinalAmount()).isEqualTo(expectedFinalAmount)
            );
        }

        @Test
        @DisplayName("PAYMENT_COMPLETED 상태에서 쿠폰 적용 시 예외가 발생한다")
        void applyCouponInPaymentCompletedStatus() {
            // given
            Order order = createDefaultOrder();
            order.completePayment();

            // when & then
            assertThrowsCustomException(
                ErrorCode.INVALID_ORDER_STATUS_APPLY_COUPON,
                () -> order.applyCoupon(1L, 5000L)
            );
        }

        @Test
        @DisplayName("쿠폰 ID와 할인 금액을 변경할 수 있다")
        void changeCouponIdAndDiscount() {
            // given
            Order order = createDefaultOrder();
            order.applyCoupon(1L, 5000L);

            // when
            order.applyCoupon(2L, 10000L);

            // then
            assertAll(
                () -> assertThat(order.getUserCouponId()).isEqualTo(2L),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(10000L),
                () -> assertThat(order.getFinalAmount()).isEqualTo(30000L)
            );
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, -100L, -1000L})
        @DisplayName("음수 할인 금액으로 쿠폰 적용 시 예외가 발생한다")
        void applyCouponWithNegativeDiscount(Long discountAmount) {
            // given
            Order order = createDefaultOrder();

            // when & then
            assertThrowsIllegalArgumentException(
                "할인 금액은 0 이상이어야 합니다.",
                () -> order.applyCoupon(1L, discountAmount)
            );
        }

        @Test
        @DisplayName("null 할인 금액으로 쿠폰 적용 시 예외가 발생한다")
        void applyCouponWithNullDiscount() {
            // given
            Order order = createDefaultOrder();

            // when & then
            assertThrowsIllegalArgumentException(
                "할인 금액은 0 이상이어야 합니다.",
                () -> order.applyCoupon(1L, null)
            );
        }

        @Test
        @DisplayName("총 주문 금액보다 큰 할인 금액으로 쿠폰 적용 시 예외가 발생한다")
        void applyCouponWithDiscountExceedingTotalAmount() {
            // given
            Order order = createDefaultOrder();
            Long totalAmount = order.getTotalAmount();

            // when & then
            assertThrowsIllegalArgumentException(
                "할인 금액이 총 주문 금액보다 클 수 없습니다.",
                () -> order.applyCoupon(1L, totalAmount + 1)
            );
        }

        @Test
        @DisplayName("쿠폰 적용 시 updatedAt이 갱신된다")
        void updatedAtIsRefreshedAfterApplyCoupon() throws InterruptedException {
            // given
            Order order = createDefaultOrder();
            var originalUpdatedAt = order.getUpdatedAt();
            Thread.sleep(10);

            // when
            order.applyCoupon(1L, 5000L);

            // then
            assertThat(order.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("결제 완료 테스트")
    class CompletePaymentTest {

        @Test
        @DisplayName("결제를 정상적으로 완료한다")
        void completePaymentSuccessfully() {
            // given
            Order order = createDefaultOrder();

            // when
            order.completePayment();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        }

        @Test
        @DisplayName("결제 완료 시 updatedAt이 갱신된다")
        void updatedAtIsRefreshedAfterCompletePayment() throws InterruptedException {
            // given
            Order order = createDefaultOrder();
            var originalUpdatedAt = order.getUpdatedAt();
            Thread.sleep(10);

            // when
            order.completePayment();

            // then
            assertThat(order.getUpdatedAt()).isAfter(originalUpdatedAt);
        }

        @Test
        @DisplayName("결제 완료 후 상태가 변경된다")
        void statusChangesAfterCompletePayment() {
            // given
            Order order = createDefaultOrder();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

            // when
            order.completePayment();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        }
    }

    @Nested
    @DisplayName("주문 취소 테스트")
    class CancelTest {

        @Test
        @DisplayName("주문을 정상적으로 취소한다")
        void cancelOrderSuccessfully() {
            // given
            Order order = createDefaultOrder();

            // when
            order.cancel();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("주문 취소 시 updatedAt이 갱신된다")
        void updatedAtIsRefreshedAfterCancel() throws InterruptedException {
            // given
            Order order = createDefaultOrder();
            var originalUpdatedAt = order.getUpdatedAt();
            Thread.sleep(10);

            // when
            order.cancel();

            // then
            assertThat(order.getUpdatedAt()).isAfter(originalUpdatedAt);
        }

        @Test
        @DisplayName("주문 취소 후 상태가 CANCELLED로 변경된다")
        void statusChangesAfterCancel() {
            // given
            Order order = createDefaultOrder();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

            // when
            order.cancel();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("주문 항목 조회 테스트")
    class GetOrderItemsTest {

        @Test
        @DisplayName("주문 항목을 조회한다")
        void getOrderItems() {
            // given
            Order order = createDefaultOrder();

            // when
            List<OrderItem> orderItems = order.getOrderItems();

            // then
            assertThat(orderItems).hasSize(2);
        }
    }
}
