package com.example.ecommerce.order.domain.status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상품 상태")
public enum OrderItemStatus {
    @Schema(description = "주문됨")
    ORDERED,

    @Schema(description = "준비중")
    PREPARING,

    @Schema(description = "배송중")
    SHIPPED,

    @Schema(description = "배송완료")
    DELIVERED,

    @Schema(description = "취소")
    CANCELLED,

    @Schema(description = "반품")
    RETURNED,

    @Schema(description = "교환")
    EXCHANGED
}
