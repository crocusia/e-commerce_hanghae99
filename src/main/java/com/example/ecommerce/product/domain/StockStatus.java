package com.example.ecommerce.product.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 재고 상태: 구매가능, 품절임박, 품절")
public enum StockStatus {
    AVAILABLE,
    LOW_STOCK,
    OUT_OF_STOCK
}
