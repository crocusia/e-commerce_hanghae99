package com.example.ecommerce.product.event;

import com.example.ecommerce.common.event.MessagePublisher;
import com.example.ecommerce.order.event.OrderCreatedEvent;
import com.example.ecommerce.payment.event.PaymentCompletedEvent;
import com.example.ecommerce.payment.event.PaymentFailedEvent;
import com.example.ecommerce.product.domain.StockReservation;
import com.example.ecommerce.product.repository.StockReservationRepository;
import com.example.ecommerce.product.service.StockService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final StockService stockService;
    private final StockReservationRepository reservationRepository;
    private final MessagePublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
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

            // 예외를 throw하지 않음 - 실패 이벤트를 커밋하기 위해
            // OrderEventListener가 ReservationFailedEvent를 처리
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - 재고 확정 시작, orderId: {}", event.orderId());

        try {
            List<StockReservation> reservations =
                reservationRepository.findPendingByOrderId(event.orderId());

            reservations.forEach(reservation -> {
                log.debug("재고 확정 - productId: {}, quantity: {}",
                    reservation.getProductId(), reservation.getQuantity());
                stockService.confirmReservation(reservation.getProductId(), reservation.getId());
            });

            log.info("재고 확정 완료 - orderId: {}", event.orderId());

        } catch (Exception e) {
            log.error("재고 확정 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("결제 실패 이벤트 수신 - 재고 해제 시작, orderId: {}", event.orderId());

        try {
            List<StockReservation> reservations =
                reservationRepository.findPendingByOrderId(event.orderId());

            reservations.forEach(reservation -> {
                log.debug("재고 해제 - productId: {}, quantity: {}",
                    reservation.getProductId(), reservation.getQuantity());
                stockService.releaseReservation(reservation.getProductId(), reservation.getId());
            });

            log.info("재고 해제 완료 - orderId: {}", event.orderId());

        } catch (Exception e) {
            log.error("재고 해제 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            throw e;
        }
    }
}
