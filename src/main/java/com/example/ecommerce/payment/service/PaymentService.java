package com.example.ecommerce.payment.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.domain.Payment;
import com.example.ecommerce.payment.dto.PaymentResponse;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.service.StockService;
import com.example.ecommerce.user.domain.User;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StockService stockService;

    @Transactional
    public PaymentResponse processPayment(Long userId, Long orderId) {

        Order order = orderRepository.findByIdOrElseThrow(orderId);

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findByIdOrElseThrow(userId);

        Payment payment = Payment.builder()
            .orderId(orderId)
            .userId(userId)
            .amount(order.getFinalAmount())
            .build();
        Payment savedPayment = paymentRepository.save(payment);
        Payment finalPayment = null;
        try {
            user.deductBalance(order.getFinalAmount());
            userRepository.save(user);

            //결제 성공 - 실제 재고 차감
            stockService.confirm(orderId);

            order.completePayment();

            savedPayment.complete();

        } catch (CustomException e) {
            // 결제 실패 - 주문 취소
            order.cancel();

            savedPayment.fail(e.getMessage());

            log.error("Payment failed for order: {}, user: {}, error: {}",
                orderId, userId, e.getMessage());

            throw e;
        } finally {
            // 예약 재고 해제 (성공/실패 모두)
            try {
                stockService.release(orderId);
            } catch (Exception releaseException) {
                log.error("Failed to release stock reservations for order: {}",
                    orderId, releaseException);
            }

            orderRepository.save(order);
            finalPayment = paymentRepository.save(savedPayment);
        }
        return PaymentResponse.from(finalPayment);
    }
}
