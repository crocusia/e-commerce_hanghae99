package com.example.ecommerce.api;

import com.example.ecommerce.dto.common.ErrorResponse;
import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.dto.coupon.IssueCouponRequest;
import com.example.ecommerce.dto.coupon.UserCouponResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "유저 쿠폰", description = "유저가 보유한 쿠폰")
public interface UserCouponApi {

    @Operation(
        summary = "쿠폰 발급 (선착순, 1인 1매)",
        description = "선착순 쿠폰을 발급합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "쿠폰 발급 성공",
            content = @Content(schema = @Schema(implementation = UserCouponResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "발급 수량 마감 또는 이미 발급받은 쿠폰",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "쿠폰 또는 사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/issue")
    ResponseEntity<UserCouponResponse> issueCoupon(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "쿠폰 발급 요청",
            required = true,
            content = @Content(schema = @Schema(implementation = IssueCouponRequest.class))
        )
        @RequestBody @Valid IssueCouponRequest request
    );

    @Operation(
        summary = "사용자 보유 쿠폰 목록 조회 (페이징)",
        description = "특정 사용자가 보유한 쿠폰 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "보유 쿠폰 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/users/{userId}")
    ResponseEntity<PageResponse<UserCouponResponse>> getUserCoupons(
        @Parameter(description = "사용자 ID")
        @PathVariable @Positive Long userId

        //@Parameter(description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬)")
        //@PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
        summary = "특정 사용자 쿠폰 조회",
        description = "특정 사용자 쿠폰의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "사용자 쿠폰 조회 성공",
            content = @Content(schema = @Schema(implementation = UserCouponResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자 쿠폰을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/users/{userId}/coupons/{userCouponId}")
    ResponseEntity<UserCouponResponse> getUserCoupon(
        @Parameter(description = "사용자 ID")
        @PathVariable @Positive Long userId,
        @Parameter(description = "사용자 쿠폰 ID")
        @PathVariable @Positive Long userCouponId
    );
}
