package com.example.ecommerce.order.event;

import com.example.ecommerce.external.dataplatform.DataPlatformClient;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.event.PaymentCompletedEvent;
import com.example.ecommerce.payment.event.PaymentFailedEvent;
import com.example.ecommerce.product.event.ReservationCompletedEvent;
import com.example.ecommerce.product.event.ReservationFailedEvent;
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
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final DataPlatformClient dataPlatformClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReservationCompleted(ReservationCompletedEvent event) {
        log.info("재고 예약 완료 이벤트 수신 - orderId: {}", event.orderId());

        try {
            Order order = orderRepository.findByIdOrElseThrow(event.orderId());
            order.completeReservation();
            orderRepository.save(order);

            log.info("주문 상태 변경 완료 - orderId: {}, status: PENDING", event.orderId());
        } catch (Exception e) {
            log.error("주문 상태 변경 실패 - orderId: {}, error: {}",
                event.orderId(), e.getMessage(), e);
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReservationFailed(ReservationFailedEvent event) {
        log.warn("재고 예약 실패 이벤트 수신 - orderId: {}, reason: {}",
            event.orderId(), event.failureReason());

        try {
            Order order = orderRepository.findByIdOrElseThrow(event.orderId());
            order.failReservation();
            orderRepository.save(order);

            log.info("주문 상태 변경 완료 - orderId: {}, status: RESERVATION_FAILED", event.orderId());
        } catch (Exception e) {
            log.error("주문 상태 변경 실패 - orderId: {}, error: {}",
                event.orderId(), e.getMessage(), e);
            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - orderId: {}", event.orderId());

        try {
            Order order = orderRepository.findByIdOrElseThrow(event.orderId());
            order.completePayment();
            orderRepository.save(order);

            log.info("주문 상태 변경 완료 - orderId: {}, status: PAYMENT_COMPLETED", event.orderId());
        } catch (Exception e) {
            log.error("주문 상태 변경 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("결제 실패 이벤트 수신 - orderId: {}, reason: {}", event.orderId(), event.failureReason());

        try {
            Order order = orderRepository.findByIdOrElseThrow(event.orderId());
            order.cancel();
            orderRepository.save(order);

            log.info("주문 상태 변경 완료 - orderId: {}, status: CANCELLED", event.orderId());
        } catch (Exception e) {
            log.error("주문 상태 변경 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 주문 생성 이벤트를 데이터 플랫폼에 전송
     * - 비동기 처리: 주문 생성 응답 시간에 영향 없음
     * - 트랜잭션 분리: 주문 트랜잭션 커밋 후 실행
     * - 실패 격리: 전송 실패가 주문 생성에 영향 없음
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedForDataPlatform(OrderCreatedEvent event) {
        log.info("데이터 플랫폼 전송 이벤트 수신 - orderId: {}", event.aggregateId());

        try {
            dataPlatformClient.sendOrderData(event);
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패 - orderId: {}, error: {}",
                event.aggregateId(), e.getMessage(), e);
            // 실패해도 주문 생성에는 영향 없음 (부가 로직)
            // 실제 환경에서는 재시도 큐, 데드레터 큐 등으로 처리
        }
    }
}
