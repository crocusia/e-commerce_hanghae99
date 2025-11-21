package com.example.ecommerce.payment.event;

import com.example.ecommerce.common.event.MessagePublisher;
import com.example.ecommerce.payment.dto.PaymentResult;
import com.example.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final MessagePublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        log.info("결제 생성 이벤트 수신 - paymentId: {}, orderId: {}",
            event.paymentId(), event.orderId());

        try {
            // 실제 결제 처리 (Service 호출)
            PaymentResult result = paymentService.processPayment(event.paymentId());

            // 결과에 따라 이벤트 발행
            if (result.success()) {
                // 결제 성공 이벤트 발행
                eventPublisher.publish(
                    PaymentCompletedEvent.of(
                        result.payment().getId(),
                        result.order().getId(),
                        result.userId(),
                        result.finalAmount()
                    )
                );
                log.info("결제 성공 이벤트 발행 - paymentId: {}, orderId: {}",
                    result.payment().getId(), result.order().getId());

            } else {
                // 결제 실패 이벤트 발행
                eventPublisher.publish(
                    PaymentFailedEvent.of(
                        result.payment().getId(),
                        result.order().getId(),
                        result.userId(),
                        result.failureReason()
                    )
                );
                log.warn("결제 실패 이벤트 발행 - paymentId: {}, orderId: {}, reason: {}",
                    result.payment().getId(), result.order().getId(), result.failureReason());
            }

        } catch (Exception e) {
            log.error("결제 처리 중 예외 발생 - paymentId: {}, orderId: {}, error: {}",
                event.paymentId(), event.orderId(), e.getMessage(), e);
            throw e;
        }
    }
}
