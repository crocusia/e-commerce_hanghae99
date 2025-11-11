package com.example.ecommerce.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "장바구니 전체 응답")
public record CartResponse(
    @Schema(description = "장바구니 상품 목록")
    List<CartItemResponse> items,

    @Schema(description = "총 상품 금액")
    Long totalAmount,

    @Schema(description = "할인 금액")
    Long discountAmount
) {
}