package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.domain.status.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "주문 응답")
public record OrderResponse(
    @Schema(description = "주문 ID")
    Long id,

    @Schema(description = "사용자 ID")
    Long userId,

    @Schema(description = "주문 상품 목록")
    List<OrderItemResponse> items,

    @Schema(description = "총 주문 금액")
    Integer totalAmount,

    @Schema(description = "할인 금액")
    Integer discountAmount,

    @Schema(description = "최종 결제 금액")
    Integer finalAmount,

    @Schema(description = "주문 상태")
    OrderStatus status,

    @Schema(description = "생성 일시")
    LocalDateTime createdAt
) {
}