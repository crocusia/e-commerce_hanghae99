package com.example.ecommerce.payment.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.domain.Payment;
import com.example.ecommerce.payment.domain.status.PaymentStatus;
import com.example.ecommerce.payment.dto.PaymentRequest;
import com.example.ecommerce.payment.dto.PaymentResult;
import com.example.ecommerce.payment.repository.PaymentRepository;
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

    @Transactional
    public Payment createPayment(PaymentRequest request) {

        Long orderId = request.orderId();
        Long userId = request.userId();

        Order order = orderRepository.findByIdOrElseThrow(orderId);

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        order.validateForPayment();

        Payment payment = Payment.builder()
            .orderId(orderId)
            .userId(userId)
            .amount(order.getFinalAmount())
            .status(PaymentStatus.PENDING)
            .build();

        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentResult processPayment(Long paymentId) {
        Payment payment = paymentRepository.findByIdOrElseThrow(paymentId);

        Long orderId = payment.getOrderId();
        Long userId = payment.getUserId();

        Order order = orderRepository.findByIdOrElseThrow(orderId);

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        order.validateForPayment();

        User user = userRepository.findByIdOrElseThrow(userId);

        try {
            // 잔액 차감 (낙관적 락)
            user.deductBalance(order.getFinalAmount());

            // 결제 완료 처리
            payment.complete();
            paymentRepository.save(payment);

            userRepository.save(user);

            log.info("결제 처리 성공 - paymentId: {}, orderId: {}, amount: {}",
                payment.getId(), order.getId(), order.getFinalAmount());

            return PaymentResult.success(payment, order, user);

        } catch (CustomException e) {

            payment.fail(e.getMessage());
            paymentRepository.save(payment);

            log.warn("결제 처리 실패 - paymentId: {}, orderId: {}, reason: {}",
                payment.getId(), order.getId(), e.getMessage());

            return PaymentResult.failure(payment, order, e.getMessage());
        }
    }
}
