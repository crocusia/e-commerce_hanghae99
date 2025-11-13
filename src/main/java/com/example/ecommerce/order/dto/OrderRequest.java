package com.example.ecommerce.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "주문 생성 요청")
public record OrderRequest(
    @Schema(description = "사용자 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "사용자 ID는 필수입니다")
    @Positive(message = "사용자 ID는 양수여야 합니다")
    Long userId,

    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다")
    @Schema(description = "주문 상품 목록")
    List<@Valid OrderItemRequest> orderItems
) {
}