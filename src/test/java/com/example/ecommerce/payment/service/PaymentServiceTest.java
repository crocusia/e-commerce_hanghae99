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
import com.example.ecommerce.payment.dto.PaymentResult;
import com.example.ecommerce.payment.repository.PaymentRepository;
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
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        OrderItem orderItem1 = OrderItem.builder()
            .productId(1L)
            .productName("상품1")
            .quantity(1)
            .unitPrice(10000L)
            .subtotal(10000L)
            .status(com.example.ecommerce.order.domain.status.OrderItemStatus.ORDERED)
            .createdAt(now)
            .updatedAt(now)
            .build();
        OrderItem orderItem2 = OrderItem.builder()
            .productId(2L)
            .productName("상품2")
            .quantity(1)
            .unitPrice(20000L)
            .subtotal(20000L)
            .status(com.example.ecommerce.order.domain.status.OrderItemStatus.ORDERED)
            .createdAt(now)
            .updatedAt(now)
            .build();

        return Order.builder()
            .id(id)
            .userId(userId)
            .totalAmount(totalAmount)
            .discountAmount(0L)
            .finalAmount(totalAmount)
            .status(com.example.ecommerce.order.domain.status.OrderStatus.PENDING)
            .orderItems(Arrays.asList(orderItem1, orderItem2))
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private User createTestUser(Long id, String name, Long balance) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return User.builder()
            .id(id)
            .name(name)
            .email(name + "@test.com")
            .balance(balance)
            .status(com.example.ecommerce.user.domain.status.UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private Payment createTestPayment(Long id, Long orderId, Long userId, Long amount) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return Payment.builder()
            .id(id)
            .orderId(orderId)
            .userId(userId)
            .amount(amount)
            .status(com.example.ecommerce.payment.domain.status.PaymentStatus.PENDING)
            .createdAt(now)
            .updatedAt(now)
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
            given(paymentRepository.findByIdOrElseThrow(testPaymentId)).willReturn(testPayment);
            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);
            given(userRepository.findByIdOrElseThrow(testUserId)).willReturn(testUser);
            given(paymentRepository.save(any(Payment.class))).willReturn(testPayment);
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // when
            PaymentResult result = paymentService.processPayment(testPaymentId);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.payment()).isNotNull();
            assertThat(result.order()).isEqualTo(testOrder);
            assertThat(result.userId()).isEqualTo(testUserId);
            assertThat(result.failureReason()).isNull();

            then(paymentRepository).should().findByIdOrElseThrow(testPaymentId);
            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
            then(userRepository).should().findByIdOrElseThrow(testUserId);
            then(paymentRepository).should(times(1)).save(any(Payment.class));
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("잔액 부족 시 결제가 실패한다")
        void processPayment_InsufficientBalance() {
            // given
            User poorUser = createTestUser(testUserId, "가난한유저", 1000L);

            given(paymentRepository.findByIdOrElseThrow(testPaymentId)).willReturn(testPayment);
            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);
            given(userRepository.findByIdOrElseThrow(testUserId)).willReturn(poorUser);
            given(paymentRepository.save(any(Payment.class))).willReturn(testPayment);

            // when
            PaymentResult result = paymentService.processPayment(testPaymentId);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.failureReason()).contains("잔액이 부족합니다");

            then(paymentRepository).should().findByIdOrElseThrow(testPaymentId);
            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
            then(userRepository).should().findByIdOrElseThrow(testUserId);
            then(paymentRepository).should(times(1)).save(any(Payment.class));
        }

        @Test
        @DisplayName("주문 소유자와 결제 요청자가 다르면 예외가 발생한다")
        void processPayment_UserMismatch() {
            // given
            Long otherUserId = 999L;
            Payment otherUserPayment = Payment.builder()
                .id(testPaymentId)
                .orderId(testOrderId)
                .userId(otherUserId)
                .amount(30000L)
                .status(com.example.ecommerce.payment.domain.status.PaymentStatus.PENDING)
                .build();

            given(paymentRepository.findByIdOrElseThrow(testPaymentId)).willReturn(otherUserPayment);
            given(orderRepository.findByIdOrElseThrow(testOrderId)).willReturn(testOrder);

            // when & then
            assertThrowsCustomException(
                ErrorCode.INVALID_INPUT_VALUE,
                () -> paymentService.processPayment(testPaymentId)
            );

            then(paymentRepository).should().findByIdOrElseThrow(testPaymentId);
            then(orderRepository).should().findByIdOrElseThrow(testOrderId);
            then(userRepository).should(never()).findByIdOrElseThrow(any());
            then(paymentRepository).should(never()).save(any(Payment.class));
        }
    }
}
