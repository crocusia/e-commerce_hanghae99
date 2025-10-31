package com.example.ecommerce.controller;

import com.example.ecommerce.api.ProductApi;
import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.dto.product.ProductResponse;
import com.example.ecommerce.dto.product.ProductStatus;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController implements ProductApi {

    @Override
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        // Mock: 빈 페이지 반환
        PageResponse<ProductResponse> response = PageResponse.empty(
            pageable.getPageNumber(),
            pageable.getPageSize()
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ProductResponse> getProduct(
        @PathVariable @Positive Long productId
    ) {
        //Mock 데이터
        ProductResponse mockResponse = new ProductResponse(
            productId,
            "Mock",
            1L,
            "Mock",
            50,
            ProductStatus.ACTIVE
        );

        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }
}