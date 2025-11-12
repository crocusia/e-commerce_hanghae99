package com.example.ecommerce.product.dto;

import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductStock;
import com.example.ecommerce.product.domain.status.StockStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상품 상세 정보 응답")
public record ProductDetailResponse(
    @Schema(description = "상품 ID") Long id,
    @Schema(description = "상품명") String name,
    @Schema(description = "가격") Long price,
    @Schema(description = "상세 설명") String comment,
    @Schema(description = "재고 상태") StockStatus status,
    @Schema(description = "재고량") Integer stock,
    @Schema(description = "생성 일시") LocalDateTime createdAt
) {
    public static ProductDetailResponse from(Product product, StockStatus status, ProductStock stock) {
        return new ProductDetailResponse(
            product.getProductId(),
            product.getName(),
            product.getPrice().getAmount(),
            product.getComment(),
            status,
            stock.getCurrentStock().getQuantity(),
            product.getCreatedAt()
        );
    }
}