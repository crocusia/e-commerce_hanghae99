package com.example.ecommerce.order.event;

import com.example.ecommerce.common.event.DomainEvent;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.domain.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String aggregateType,
    Long aggregateId,
    Long userId,
    Long userCouponId,
    Long totalAmount,
    Long discountAmount,
    Long finalAmount,
    List<OrderItemInfo> orderItems
) implements DomainEvent {

    public record OrderItemInfo(
        Long productId,
        Integer quantity
    ) {
        public static OrderItemInfo from(OrderItem orderItem) {
            return new OrderItemInfo(
                orderItem.getProductId(),
                orderItem.getQuantity()
            );
        }
    }

    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
            UUID.randomUUID().toString(),
            "OrderCreated",
            LocalDateTime.now(),
            "Order",
            order.getId(),
            order.getUserId(),
            order.getUserCouponId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getFinalAmount(),
            order.getOrderItems().stream()
                .map(OrderItemInfo::from)
                .toList()
        );
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public Long getAggregateId() {
        return aggregateId;
    }
}
