package com.example.ecommerce.order.domain;

import com.example.ecommerce.order.domain.status.OrderItemStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OrderItem {
    private final Long id;
    private final Long orderId;
    private final Long productId;
    private final String productName;
    private Integer quantity;
    private final Long unitPrice;
    private Long subtotal;
    private OrderItemStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private OrderItem(Long id, Long orderId, Long productId, String productName,
        Integer quantity, Long unitPrice) {

        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = quantity * unitPrice;
        this.status = OrderItemStatus.ORDERED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static OrderItem create(Long productId, String productName,
        Integer quantity, Long unitPrice) {
        return OrderItem.builder()
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
    }
}
