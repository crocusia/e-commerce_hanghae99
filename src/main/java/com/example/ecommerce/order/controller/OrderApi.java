package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.exception.ErrorResponse;
import com.example.ecommerce.common.dto.PageResponse;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.payment.dto.PaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "주문 및 결제", description = "주문 생성 및 결제 처리 API")
public interface OrderApi {

    @Operation(
        summary = "주문 목록 조회 (페이징)",
        description = """
            사용자의 전체 주문 목록을 페이징하여 조회합니다.

            **페이징 파라미터:**
            - `page`: 페이지 번호 (0부터 시작, 기본값: 0)
            - `size`: 페이지 크기 (기본값: 20, 최대: 100)
            - `sort`: 정렬 조건 (기본값: createdAt,desc - 최신순)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 목록 조회 성공",
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
    @GetMapping
    ResponseEntity<PageResponse<OrderResponse>> getOrders(
        @Parameter(description = "사용자 ID")
        @RequestParam @Positive Long userId,

        @Parameter(description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬)")
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
        summary = "주문 상세 조회",
        description = "특정 주문의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 조회 성공",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{orderId}")
    ResponseEntity<OrderResponse> getOrder(
        @Parameter(description = "주문 ID")
        @PathVariable @Positive Long orderId
    );

    @Operation(
        summary = "주문 생성",
        description = "장바구니 상품을 기반으로 주문을 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "주문 생성 성공",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "재고 부족, 장바구니 비어있음, 또는 잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자 또는 쿠폰을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    ResponseEntity<OrderResponse> createOrder(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "주문 생성 요청",
            required = true,
            content = @Content(schema = @Schema(implementation = OrderRequest.class))
        )
        @RequestBody @Valid OrderRequest request
    );

    @Operation(
        summary = "결제 처리",
        description = "주문에 대한 결제를 처리합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "결제 성공",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잔액 부족, 이미 결제된 주문, 또는 쿠폰 사용 조건 불충족",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문 또는 쿠폰을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/payment")
    ResponseEntity<OrderResponse> processPayment(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "결제 요청",
            required = true,
            content = @Content(schema = @Schema(implementation = PaymentRequest.class))
        )
        @RequestBody @Valid PaymentRequest request
    );

    @Operation(
        summary = "주문 취소",
        description = "결제 대기 중인 주문을 취소합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 취소 성공",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "이미 결제된 주문은 취소 불가",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/{orderId}/cancel")
    ResponseEntity<OrderResponse> cancelOrder(
        @Parameter(description = "주문 ID")
        @PathVariable @Positive Long orderId
    );
}
