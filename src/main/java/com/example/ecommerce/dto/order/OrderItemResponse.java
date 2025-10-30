package com.example.ecommerce.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상품 응답")
public record OrderItemResponse(
    @Schema(description = "주문 상품 ID")
    Long id,

    @Schema(description = "주문 ID")
    Long orderId,

    @Schema(description = "상품 ID")
    Long productId,

    @Schema(description = "상품명")
    String productName,

    @Schema(description = "수량")
    Integer quantity,

    @Schema(description = "단가")
    Integer unitPrice,

    @Schema(description = "소계")
    Integer subtotal,

    @Schema(description = "주문 상품 상태")
    OrderItemStatus status
) {
}