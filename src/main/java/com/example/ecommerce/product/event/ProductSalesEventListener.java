package com.example.ecommerce.product.event;

import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.domain.OrderItem;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.event.PaymentCompletedEvent;
import com.example.ecommerce.product.service.ProductSalesRedisService;
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
public class ProductSalesEventListener {

    private final ProductSalesRedisService salesRedisService;
    private final OrderRepository orderRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - 판매 수 집계 시작, orderId: {}", event.orderId());

        try {
            Order order = orderRepository.findByIdOrElseThrow(event.orderId());

            for (OrderItem item : order.getOrderItems()) {
                salesRedisService.incrementSales(
                    item.getProductId(),
                    item.getQuantity()
                );

                log.debug("상품 판매 수 증가 완료 - productId: {}, quantity: {}",
                    item.getProductId(), item.getQuantity());
            }

            log.info("판매 수 집계 완료 - orderId: {}, 상품 수: {}",
                event.orderId(), order.getOrderItems().size());

        } catch (Exception e) {
            log.error("판매 수 집계 실패 - orderId: {}, error: {}",
                event.orderId(), e.getMessage(), e);
        }
    }
}
