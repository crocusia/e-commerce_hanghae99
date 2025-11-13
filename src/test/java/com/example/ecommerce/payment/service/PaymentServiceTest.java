package com.example.ecommerce.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.domain.OrderItem;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.domain.Payment;
import com.example.ecommerce.payment.dto.PaymentResponse;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.service.StockService;
import com.example.ecommerce.user.domain.User;
import com.example.ecommerce.user.repository.UserRepository;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockService stockService;

    @InjectMocks
    private PaymentService paymentService;

    // 테스트 데이터
    private Long testUserId;
    private Long testOrderId;
    private Long testPaymentId;

    private Order testOrder;
    private User testUser;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testOrderId = 1L;
        testPaymentId = 1L;

        testOrder = createTestOrder(testOrderId, testUserId, 30000L);
        testUser = createTestUser(testUserId, "테스트유저", 100000L);
        testPayment = createTestPayment(testPaymentId, testOrderId, testUserId, 30000L);
    }

    // 헬퍼 메서드
    private Order createTestOrder(Long id, Long userId, Long totalAmount) {
        OrderItem orderItem1 = OrderItem.create(1L, "상품1", 1, 10000L);
        OrderItem orderItem2 = OrderItem.create(2L, "상품2", 1, 20000L);

        return Order.builder()
            .id(id)
            .userId(userId)
            .orderItems(Arrays.asList(orderItem1, orderItem2))
            .build();
    }

    private User createTestUser(Long id, String name, Long balance) {
        return User.builder()
            .id(id)
            .name(name)
            .email(name + "@test.com")
            .balance(balance)
            .build();
    }

    private Payment createTestPayment(Long id, Long orderId, Long userId, Long amount) {
        return Payment.builder()
            .id(id)
            .orderId(orderId)
            .userId(userId)
            .amount(amount)
            .build();
    }

    private void assertThrowsCustomException(ErrorCode expectedErrorCode, Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(expectedErrorCode);
    }

    @Nested
    @DisplayName("결제 처리 테스트")
    class ProcessPaymentTest {

        @Test
        @DisplayName("결제를 정상적으로 처리한다")
        void processPayment_Success() {
            // given
            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);
            given(userRepository.findByIdOrElseThrow(testUserId)).willReturn(testUser);
            given(paymentRepository.save(any(Payment.class))).willReturn(testPayment);
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(orderRepository.save(any(Order.class))).willReturn(testOrder);

            // when
            PaymentResponse response = paymentService.processPayment(testUserId, testOrderId);

            // then
            assertThat(response).isNotNull();

            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
            then(userRepository).should().findByIdOrElseThrow(testUserId);
            then(paymentRepository).should(times(2)).save(any(Payment.class));
            then(userRepository).should().save(any(User.class));
            then(stockService).should().confirm(testOrderId);
            then(stockService).should().release(testOrderId);
            then(orderRepository).should().save(any(Order.class));
        }

        @Test
        @DisplayName("잔액 부족 시 결제가 실패하고 주문이 취소된다")
        void processPayment_InsufficientBalance() {
            // given
            User poorUser = createTestUser(testUserId, "가난한유저", 1000L);

            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);
            given(userRepository.findByIdOrElseThrow(testUserId)).willReturn(poorUser);

            given(paymentRepository.save(any(Payment.class))).willReturn(testPayment);
            given(orderRepository.save(any(Order.class))).willReturn(testOrder);

            assertThrowsCustomException(
                ErrorCode.USER_INSUFFICIENT_BALANCE,
                () -> paymentService.processPayment(testUserId, testOrderId)
            );

            // then
            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
            then(userRepository).should().findByIdOrElseThrow(testUserId);

            then(paymentRepository).should(times(2)).save(any(Payment.class));
            then(stockService).should(never()).confirm(anyLong());
            then(stockService).should().release(testOrderId);
            then(orderRepository).should().save(any(Order.class));
        }

        @Test
        @DisplayName("주문 소유자와 결제 요청자가 다르면 예외가 발생한다")
        void processPayment_UserMismatch() {
            // given
            Long otherUserId = 999L;
            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);

            // when & then
            assertThrowsCustomException(
                ErrorCode.INVALID_INPUT_VALUE,
                () -> paymentService.processPayment(otherUserId, testOrderId)
            );

            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
            then(userRepository).should(never()).findByIdOrElseThrow(any());
            then(paymentRepository).should(never()).save(any(Payment.class));
            then(stockService).should(never()).confirm(anyLong());
            then(stockService).should(never()).release(anyLong());
        }
    }
}
