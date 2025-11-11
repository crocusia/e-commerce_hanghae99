package com.example.ecommerce.product.controller;

import com.example.ecommerce.common.dto.PageResponse;
import com.example.ecommerce.product.dto.ProductDetailResponse;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.service.ProductService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductService productService;

    @Override
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse> pageData = productService.getActiveProducts(pageable);
        PageResponse<ProductResponse> responseData = PageResponse.from(pageData);
        return ResponseEntity.ok(responseData);
    }

    @Override
    public ResponseEntity<ProductDetailResponse> getProduct(
        @PathVariable @Positive Long productId
    ) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ProductResponse>> getPopularProducts(
        @RequestParam(defaultValue = "100") @Positive int limit
    ) {
        List<ProductResponse> response = productService.getPopularProducts(limit);
        return ResponseEntity.ok(response);
    }
}