package com.example.ecommerce.payment.orchestrator;

import com.example.ecommerce.common.event.MessagePublisher;
import com.example.ecommerce.payment.domain.Payment;
import com.example.ecommerce.payment.dto.PaymentRequest;
import com.example.ecommerce.payment.dto.PaymentResponse;
import com.example.ecommerce.payment.event.PaymentCreatedEvent;
import com.example.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrator {

    private final PaymentService paymentService;
    private final MessagePublisher eventPublisher;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("결제 생성 시작 - userId: {}, orderId: {}", request.userId(), request.orderId());

        try {
            Payment payment = paymentService.createPayment(request);

            log.debug("결제 생성 이벤트 발행 - paymentId: {}", payment.getId());
            PaymentCreatedEvent event = PaymentCreatedEvent.from(payment);
            eventPublisher.publish(event);

            return PaymentResponse.from(payment);

        } catch (Exception e) {
            log.error("결제 생성 실패 - userId: {}, orderId: {}, error: {}",
                request.userId(), request.orderId(), e.getMessage(), e);
            throw e;
        }
    }
}
