package com.example.ecommerce.order.domain;

import com.example.ecommerce.common.domain.BaseEntity;
import com.example.ecommerce.order.domain.status.OrderItemStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class OrderItem extends BaseEntity {

    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "subtotal", nullable = false)
    private Long subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderItemStatus status;

    public static OrderItem create(Long productId, String productName,
        Integer quantity, Long unitPrice) {
        return OrderItem.builder()
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .subtotal(quantity * unitPrice)
            .status(OrderItemStatus.ORDERED)
            .build();
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
