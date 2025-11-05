package com.example.ecommerce.product.controller;

import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.product.service.ProductService.ProductDetailOutPut;
import com.example.ecommerce.product.service.ProductService.ProductInPut;
import com.example.ecommerce.product.service.ProductService.ProductOutPut;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

@Validated
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping
    public ResponseEntity<ProductOutPut> registerProduct(@Valid @RequestBody ProductInPut input) {
        ProductOutPut output = productService.registerProduct(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    // 판매 중인 상품 목록 조회
    @GetMapping
    public ResponseEntity<PageResponse<ProductOutPut>> getProducts(Pageable pageable) {
        Page<ProductOutPut> outputs = productService.getActiveProducts(pageable);

        PageResponse<ProductOutPut> response = PageResponse.from(outputs);

        return ResponseEntity.ok(response);
    }

    // 상품의 상세 내용 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailOutPut> getProductDetail(
        @PathVariable @NotNull @Positive Long productId
    ){
        ProductDetailOutPut outPut = productService.getProductDetail(productId);
        return ResponseEntity.ok(outPut);
    }
}