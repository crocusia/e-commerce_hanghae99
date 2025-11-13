package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.domain.OrderItem;
import com.example.ecommerce.order.domain.status.OrderItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상품 응답")
public record OrderItemResponse(
    @Schema(description = "주문 상품 ID") Long id,
    @Schema(description = "주문 ID") Long orderId,
    @Schema(description = "상품 ID") Long productId,
    @Schema(description = "상품명") String productName,
    @Schema(description = "수량") Integer quantity,
    @Schema(description = "단가") Long unitPrice,
    @Schema(description = "소계") Long subtotal,
    @Schema(description = "주문 상품 상태") OrderItemStatus status
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getOrderId(),
            item.getProductId(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getSubtotal(),
            item.getStatus()
        );
    }
}