package com.example.ecommerce.cart.dto;

import com.example.ecommerce.product.domain.status.StockStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 상품 응답")
public record CartItemResponse(
    @Schema(description = "장바구니 상품 ID")
    Long id,

    @Schema(description = "상품 ID")
    Long productId,

    @Schema(description = "상품명")
    String productName,

    @Schema(description = "상품 가격")
    Long  productPrice,

    @Schema(description = "수량")
    Integer quantity,

    @Schema(description = "소계")
    Long subtotal,

    @Schema(description = "재고 상태")
    StockStatus stockStatus
) {
}