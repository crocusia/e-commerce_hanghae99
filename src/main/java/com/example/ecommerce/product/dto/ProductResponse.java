package com.example.ecommerce.product.dto;

import com.example.ecommerce.product.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상품 응답")
public record ProductResponse(
    @Schema(description = "상품 ID") Long id,
    @Schema(description = "상품명") String name,
    @Schema(description = "가격") Long price,
    @Schema(description = "생성 일시") LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getProductId(),
            product.getName(),
            product.getPrice().getAmount(),
            product.getCreatedAt()
        );
    }
}