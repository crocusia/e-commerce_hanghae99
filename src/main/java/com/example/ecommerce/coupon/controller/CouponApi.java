package com.example.ecommerce.coupon.controller;

import com.example.ecommerce.common.exception.ErrorResponse;
import com.example.ecommerce.common.dto.PageResponse;
import com.example.ecommerce.coupon.dto.CouponResponse;
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

@Tag(name = "쿠폰", description = "선착순 쿠폰 시스템 API")
public interface CouponApi {

    @Operation(
        summary = "쿠폰 목록 조회 (페이징)",
        description = """
            전체 쿠폰 목록을 페이징하여 조회합니다.

            **페이징 파라미터:**
            - `page`: 페이지 번호 (0부터 시작, 기본값: 0)
            - `size`: 페이지 크기 (기본값: 20, 최대: 100)
            - `sort`: 정렬 조건 (기본값: validUntil,asc)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "쿠폰 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class)
            )
        )
    })
    @GetMapping
    ResponseEntity<PageResponse<CouponResponse>> getAvailableCoupons(
        @Parameter(
            description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬)"
        )
        @PageableDefault(size = 20, sort = "validUntil", direction = Sort.Direction.ASC) Pageable pageable
    );

    @Operation(
        summary = "특정 쿠폰 조회",
        description = "특정 쿠폰의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "쿠폰 조회 성공",
            content = @Content(schema = @Schema(implementation = CouponResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "쿠폰을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{couponId}")
    ResponseEntity<CouponResponse> getCoupon(
        @Parameter(description = "쿠폰 ID")
        @PathVariable @Positive Long couponId
    );
}
