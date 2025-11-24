package com.example.ecommerce.order.orchestrator;

import com.example.ecommerce.common.event.MessagePublisher;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.event.OrderCreatedEvent;
import com.example.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 생성 플로우를 조율하는 Orchestrator
 * MSA 전환 시 Saga Orchestrator로 진화 가능
 *
 * 주문 생성 플로우:
 * 1. 주문 엔티티 생성 (동기, 트랜잭션 내)
 * 2. 재고 예약 이벤트 발행 (비동기)
 *
 * 쿠폰은 주문 생성 후 PATCH 요청을 통해 적용 (OrderService.applyCoupon)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationOrchestrator {

    private final OrderService orderService;
    private final MessagePublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("주문 생성 시작 - userId: {}", request.userId());

        try {
            Order order = orderService.createOrderEntity(request);

            log.debug("주문 생성 이벤트 발행 - orderId: {}", order.getId());
            OrderCreatedEvent event = OrderCreatedEvent.from(order);
            eventPublisher.publish(event);

            return OrderResponse.from(order);

        } catch (Exception e) {
            log.error("주문 생성 실패 - userId: {}, error: {}", request.userId(), e.getMessage(), e);
            throw e;
        }
    }
}
