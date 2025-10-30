package com.example.ecommerce.controller;

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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "주문 상품", description = "주문 상품 관리 API")
@Validated
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

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
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(
        @Parameter(description = "주문 ID")
        @PathVariable @Positive Long orderId
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(List.of());
    }

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
    public ResponseEntity<OrderItemResponse> getOrderItem(
        @Parameter(description = "주문 상품 ID")
        @PathVariable @Positive Long orderItemId
    ) {
        // Mock 데이터
        OrderItemResponse mockResponse = new OrderItemResponse(
            orderItemId,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }

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
    public ResponseEntity<OrderItemResponse> cancelOrderItem(
        @Parameter(description = "주문 상품 ID")
        @PathVariable @Positive Long orderItemId
    ) {
        // Mock 데이터
        OrderItemResponse mockResponse = new OrderItemResponse(
            orderItemId,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }

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
    public ResponseEntity<OrderItemResponse> returnOrderItem(
        @Parameter(description = "주문 상품 ID")
        @PathVariable @Positive Long orderItemId
    ) {
        // Mock 데이터
        OrderItemResponse mockResponse = new OrderItemResponse(
            orderItemId,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }

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
    public ResponseEntity<OrderItemResponse> exchangeOrderItem(
        @Parameter(description = "주문 상품 ID")
        @PathVariable @Positive Long orderItemId
    ) {
        // Mock 데이터
        OrderItemResponse mockResponse = new OrderItemResponse(
            orderItemId,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }
}
