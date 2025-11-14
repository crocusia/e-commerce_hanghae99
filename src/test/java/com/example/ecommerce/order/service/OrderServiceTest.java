package com.example.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.domain.vo.CouponQuantity;
import com.example.ecommerce.coupon.domain.vo.DiscountValue;
import com.example.ecommerce.coupon.domain.vo.ValidPeriod;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.domain.OrderItem;
import com.example.ecommerce.order.dto.OrderItemRequest;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.status.ProductStatus;
import com.example.ecommerce.product.domain.vo.Money;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.service.StockService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockService stockService;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private OrderService orderService;

    // 테스트 데이터
    private Long testUserId;
    private Long testOrderId;
    private Long testProductId1;
    private Long testProductId2;
    private Long testUserCouponId;
    private Long testCouponId;

    private Product testProduct1;
    private Product testProduct2;
    private Order testOrder;
    private UserCoupon testUserCoupon;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testOrderId = 1L;
        testProductId1 = 1L;
        testProductId2 = 2L;
        testUserCouponId = 1L;
        testCouponId = 1L;

        testProduct1 = createTestProduct(testProductId1, "상품1", 10000L, ProductStatus.ACTIVE);
        testProduct2 = createTestProduct(testProductId2, "상품2", 20000L, ProductStatus.ACTIVE);
        testOrder = createTestOrder(testOrderId, testUserId, 30000L);
        testUserCoupon = createTestUserCoupon(testUserCouponId, testUserId, testCoupon,
            LocalDateTime.now().plusDays(30));
        testCoupon = createTestCoupon(testCouponId, "테스트 쿠폰", 3000L, 10000L);
    }

    // 헬퍼 메서드
    private Product createTestProduct(Long id, String name, Long price, ProductStatus status) {
        return Product.builder()
            .id(id)
            .name(name)
            .price(Money.of(price))
            .productStatus(status)
            .build();
    }

    private Order createTestOrder(Long id, Long userId, Long totalAmount) {
        OrderItem orderItem1 = OrderItem.create(testProductId1, "상품1", 1, 10000L);
        OrderItem orderItem2 = OrderItem.create(testProductId2, "상품2", 1, 20000L);

        return Order.builder()
            .id(id)
            .userId(userId)
            .orderItems(Arrays.asList(orderItem1, orderItem2))
            .build();
    }

    private UserCoupon createTestUserCoupon(Long id, Long userId, Coupon coupon,
        LocalDateTime expiresAt) {
        return UserCoupon.builder()
            .id(id)
            .userId(userId)
            .coupon(coupon)
            .expiresAt(expiresAt)
            .build();
    }

    private Coupon createTestCoupon(Long id, String name, Long discountAmount,
        Long minOrderAmount) {
        return Coupon.builder()
            .id(id)
            .name(name)
            .discountValue(DiscountValue.fixed(discountAmount))
            .quantity(CouponQuantity.of(100))
            .validPeriod(ValidPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30)))
            .minOrderAmount(Money.of(minOrderAmount))
            .build();
    }

    private OrderRequest createOrderRequest(Long userId, List<OrderItemRequest> orderItems) {
        return new OrderRequest(userId, orderItems);
    }

    private OrderItemRequest createOrderItemRequest(Long productId, Integer quantity) {
        return new OrderItemRequest(productId, quantity);
    }

    private void assertThrowsCustomException(ErrorCode expectedErrorCode, Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(expectedErrorCode);
    }

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTest {

        @Test
        @DisplayName("주문을 정상적으로 생성한다")
        void createOrder_Success() {
            // given
            OrderItemRequest item1 = createOrderItemRequest(testProductId1, 1);
            OrderItemRequest item2 = createOrderItemRequest(testProductId2, 1);
            OrderRequest request = createOrderRequest(testUserId, Arrays.asList(item1, item2));

            given(productRepository.findByIdOrElseThrow(testProductId1)).willReturn(testProduct1);
            given(productRepository.findByIdOrElseThrow(testProductId2)).willReturn(testProduct2);
            given(orderRepository.save(any(Order.class))).willReturn(testOrder);

            // when
            OrderResponse response = orderService.createOrder(request);

            // then
            assertThat(response).isNotNull();

            then(productRepository).should().findByIdOrElseThrow(testProductId1);
            then(productRepository).should().findByIdOrElseThrow(testProductId2);
            then(orderRepository).should().save(any(Order.class));
            then(stockService).should().reserve(testOrderId, testProductId1, 1);
            then(stockService).should().reserve(testOrderId, testProductId2, 1);
        }

        @Test
        @DisplayName("상품이 판매 불가 상태이면 예외가 발생한다")
        void createOrder_ProductNotAvailable() {
            // given
            Product inactiveProduct = createTestProduct(testProductId1, "상품1", 10000L,
                ProductStatus.INACTIVE);
            OrderItemRequest item1 = createOrderItemRequest(testProductId1, 1);
            OrderRequest request = createOrderRequest(testUserId, Arrays.asList(item1));

            given(productRepository.findByIdOrElseThrow(testProductId1)).willReturn(
                inactiveProduct);

            // when & then
            assertThrowsCustomException(
                ErrorCode.INVALID_PRODUCT_STATUS,
                () -> orderService.createOrder(request)
            );

            then(productRepository).should().findByIdOrElseThrow(testProductId1);
            then(orderRepository).should(never()).save(any(Order.class));
            then(stockService).should(never()).reserve(anyLong(), anyLong(), anyInt());
        }

        @Test
        @DisplayName("재고 예약 실패 시 주문을 삭제하고 예외를 발생시킨다")
        void createOrder_StockReservationFailed() {
            // given
            OrderItemRequest item1 = createOrderItemRequest(testProductId1, 1);
            OrderRequest request = createOrderRequest(testUserId, Arrays.asList(item1));

            given(productRepository.findByIdOrElseThrow(testProductId1)).willReturn(testProduct1);
            given(orderRepository.save(any(Order.class))).willReturn(testOrder);
            willThrow(new CustomException(ErrorCode.PRODUCT_OUT_OF_STOCK))
                .given(stockService).reserve(testOrderId, testProductId1, 1);

            // when & then
            assertThrowsCustomException(
                ErrorCode.PRODUCT_OUT_OF_STOCK,
                () -> orderService.createOrder(request)
            );

            then(productRepository).should().findByIdOrElseThrow(testProductId1);
            then(orderRepository).should().save(any(Order.class));
            then(stockService).should().reserve(testOrderId, testProductId1, 1);
            then(stockService).should().release(testOrderId);
            then(orderRepository).should().delete(testOrderId);
        }
    }

    @Nested
    @DisplayName("쿠폰 적용 테스트")
    class ApplyCouponTest {

        @Test
        @DisplayName("주문에 쿠폰을 정상적으로 적용한다")
        void applyCoupon_Success() {
            // given
            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);
            given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(
                testUserCoupon);
            given(couponRepository.findByIdOrElseThrow(testCouponId)).willReturn(testCoupon);
            given(orderRepository.save(any(Order.class))).willReturn(testOrder);
            given(userCouponRepository.save(any(UserCoupon.class))).willReturn(testUserCoupon);

            // when
            OrderResponse response = orderService.applyCoupon(testOrderId, testUserCouponId);

            // then
            assertThat(response).isNotNull();

            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
            then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
            then(couponRepository).should().findByIdOrElseThrow(testCouponId);
            then(orderRepository).should().save(any(Order.class));
            then(userCouponRepository).should().save(any(UserCoupon.class));
        }

        @Test
        @DisplayName("쿠폰 소유자와 주문자가 다르면 예외가 발생한다")
        void applyCoupon_UserMismatch() {
            // given
            UserCoupon otherUserCoupon = createTestUserCoupon(testUserCouponId, 999L, testCoupon,
                LocalDateTime.now().plusDays(30));

            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);
            given(userCouponRepository.findByIdOrElseThrow(testUserCouponId)).willReturn(
                otherUserCoupon);

            // when & then
            assertThrowsCustomException(
                ErrorCode.INVALID_INPUT_VALUE,
                () -> orderService.applyCoupon(testOrderId, testUserCouponId)
            );

            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
            then(userCouponRepository).should().findByIdOrElseThrow(testUserCouponId);
            then(couponRepository).should(never()).findByIdOrElseThrow(any());
            then(orderRepository).should(never()).save(any(Order.class));
            then(userCouponRepository).should(never()).save(any(UserCoupon.class));
        }
    }

    @Nested
    @DisplayName("주문 조회 테스트")
    class GetOrderTest {

        @Test
        @DisplayName("주문을 정상적으로 조회한다")
        void getOrder_Success() {
            // given
            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);

            // when
            OrderResponse response = orderService.getOrder(testOrderId);

            // then
            assertThat(response).isNotNull();

            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외가 발생한다")
        void getOrder_NotFound() {
            // given
            given(orderRepository.findByIdOrElseThrow(testOrderId))
                .willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));

            // when & then
            assertThrowsCustomException(
                ErrorCode.ORDER_NOT_FOUND,
                () -> orderService.getOrder(testOrderId)
            );

            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
        }
    }
}
