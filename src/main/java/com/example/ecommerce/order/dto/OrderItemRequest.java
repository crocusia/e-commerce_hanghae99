package com.example.ecommerce.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 상품 요청")
public record OrderItemRequest(
    @NotNull(message = "상품 ID는 필수입니다")
    @Schema(description = "상품 ID")
    Long productId,

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    @Schema(description = "수량")
    Integer quantity
) {
}