package com.example.ecommerce.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 응답")
public record ProductResponse(
    @Schema(description = "상품 ID") Long id,
    @Schema(description = "상품명") String name,
    @Schema(description = "가격") Long price,
    @Schema(description = "상세 설명") String description,
    @Schema(description = "재고 수량") Integer stockQty,
    @Schema(description = "상품 상태") ProductStatus status
) {
}