package com.example.ecommerce.product.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 판매 상태: 판매중, 판매중지, 삭제됨")
public enum ProductStatus {
    ACTIVE,
    INACTIVE,
    DELETED
}
