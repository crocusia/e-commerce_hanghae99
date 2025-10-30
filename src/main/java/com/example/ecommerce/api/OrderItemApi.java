package com.example.ecommerce.api;

import com.example.ecommerce.dto.common.ErrorResponse;
import com.example.ecommerce.dto.order.OrderItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "주문 상품", description = "주문 상품 관리 API")
public interface OrderItemApi {

    @Operation(
        summary = "주문의 상품 목록 조회",
        description = "특정 주문에 포함된 모든 상품 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 상품 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = OrderItemResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/orders/{orderId}")
    ResponseEntity<List<OrderItemResponse>> getOrderItems(
        @Parameter(description = "주문 ID")
        @PathVariable @Positive Long orderId
    );

    @Operation(
        summary = "주문 상품 상세 조회",
        description = "특정 주문 상품의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 상품 조회 성공",
            content = @Content(schema = @Schema(implementation = OrderItemResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문 상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{orderItemId}")
    ResponseEntity<OrderItemResponse> getOrderItem(
        @Parameter(description = "주문 상품 ID")
        @PathVariable @Positive Long orderItemId
    );

    @Operation(
        summary = "주문 상품 취소",
        description = "ORDERED, PREPARING 상태의 주문 상품을 취소합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 상품 취소 성공",
            content = @Content(schema = @Schema(implementation = OrderItemResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "취소 불가능한 상태 (이미 배송중이거나 배송완료)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문 상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/{orderItemId}/cancel")
    ResponseEntity<OrderItemResponse> cancelOrderItem(
        @Parameter(description = "주문 상품 ID")
        @PathVariable @Positive Long orderItemId
    );

    @Operation(
        summary = "주문 상품 반품",
        description = "배송 완료된 주문 상품을 반품합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 상품 반품 성공",
            content = @Content(schema = @Schema(implementation = OrderItemResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "반품 불가능한 상태 (배송완료 상태가 아님)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문 상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/{orderItemId}/return")
    ResponseEntity<OrderItemResponse> returnOrderItem(
        @Parameter(description = "주문 상품 ID")
        @PathVariable @Positive Long orderItemId
    );

    @Operation(
        summary = "주문 상품 교환",
        description = "배송 완료된 주문 상품을 교환합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 상품 교환 성공",
            content = @Content(schema = @Schema(implementation = OrderItemResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "교환 불가능한 상태 (배송완료 상태가 아님) 또는 교환 상품 재고 부족",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문 상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/{orderItemId}/exchange")
    ResponseEntity<OrderItemResponse> exchangeOrderItem(
        @Parameter(description = "주문 상품 ID")
        @PathVariable @Positive Long orderItemId
    );
}
