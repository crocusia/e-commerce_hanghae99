package com.example.ecommerce.api;

import com.example.ecommerce.dto.common.ErrorResponse;
import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.dto.product.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "상품", description = "상품 카탈로그 및 재고 관리 API")
public interface ProductApi {

    @Operation(
        summary = "상품 목록 조회 (페이징)",
        description = """
            전체 상품 목록을 페이징하여 조회합니다.
            **페이징 파라미터:**
            - `page`: 페이지 번호 (0부터 시작, 기본값: 0)
            - `size`: 페이지 크기 (기본값: 20, 최대: 100)
            - `sort`: 정렬 조건 (예: `name,asc` 또는 `createdAt,desc`)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "상품 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class)
            )
        )
    })
    @GetMapping
    ResponseEntity<PageResponse<ProductResponse>> getProducts(
        @Parameter(
            description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬)",
            example = "page=0&size=20&sort=name,asc"
        )
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    );

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
    ResponseEntity<ProductResponse> getProduct(
        @Parameter(description = "상품 ID")
        @PathVariable @Positive Long productId
    );
}
