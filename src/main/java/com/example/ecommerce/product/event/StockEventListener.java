package com.example.ecommerce.product.event;

import com.example.ecommerce.common.event.MessagePublisher;
import com.example.ecommerce.order.event.OrderCreatedEvent;
import com.example.ecommerce.payment.event.PaymentCompletedEvent;
import com.example.ecommerce.payment.event.PaymentFailedEvent;
import com.example.ecommerce.product.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final StockService stockService;
    private final MessagePublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("재고 예약 시작 - orderId: {}, items: {}", event.aggregateId(), event.orderItems().size());

        try {
            event.orderItems().forEach(item -> {
                log.debug("재고 예약 - productId: {}, quantity: {}", item.productId(), item.quantity());
                stockService.reserve(event.aggregateId(), item.productId(), item.quantity());
            });

            log.info("재고 예약 완료 - orderId: {}", event.aggregateId());

            ReservationCompletedEvent completedEvent = ReservationCompletedEvent.of(event.aggregateId());
            eventPublisher.publish(completedEvent);

        } catch (Exception e) {
            log.error("재고 예약 실패 - orderId: {}, error: {}", event.aggregateId(), e.getMessage(), e);

            ReservationFailedEvent failedEvent = ReservationFailedEvent.forStock(
                event.aggregateId(),
                e.getMessage()
            );
            eventPublisher.publish(failedEvent);

            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - 재고 확정 시작, orderId: {}", event.orderId());

        try {
            stockService.confirm(event.orderId());
            log.info("재고 확정 완료 - orderId: {}", event.orderId());

        } catch (Exception e) {
            log.error("재고 확정 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("결제 실패 이벤트 수신 - 재고 해제 시작, orderId: {}", event.orderId());

        try {
            stockService.release(event.orderId());
            log.info("재고 해제 완료 - orderId: {}", event.orderId());

        } catch (Exception e) {
            log.error("재고 해제 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            throw e;
        }
    }
}
