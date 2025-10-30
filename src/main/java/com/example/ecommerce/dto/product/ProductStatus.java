package com.example.ecommerce.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 상태: 판매중, 판매중지, 삭제")
public enum ProductStatus { ACTIVE, INACTIVE, DELETED }