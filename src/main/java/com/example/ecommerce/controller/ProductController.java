package com.example.ecommerce.controller;

import com.example.ecommerce.dto.common.ErrorResponse;
import com.example.ecommerce.dto.product.ProductResponse;
import com.example.ecommerce.dto.product.ProductStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Tag(name = "상품", description = "상품 카탈로그 및 재고 관리 API")
@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Operation(
        summary = "상품 목록 조회",
        description = "전체 상품 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "상품 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ProductResponse.class))
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(List.of());
    }

    @Operation(
        summary = "상품 상세 조회",
        description = "특정 상품의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "상품 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProductResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "상품을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
        @Parameter(description = "상품 ID")
        @PathVariable @Positive Long productId
    ) {
        //Mock 데이터
        ProductResponse mockResponse = new ProductResponse(
            productId,
            "Mock 상품 노트북",
            1500000,
            "Mock",
            50,
            ProductStatus.ACTIVE
        );

        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }
}