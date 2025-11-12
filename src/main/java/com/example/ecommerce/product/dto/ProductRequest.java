package com.example.ecommerce.product.dto;

import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.vo.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "상품 등록 요청")
public record ProductRequest(
    @NotBlank(message = "상품명은 필수입니다")
    @Schema(description = "상품명")
    String name,

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Schema(description = "가격")
    Long price,

    @NotNull(message = "재고는 필수입니다")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다")
    @Schema(description = "재고")
    Integer stock,

    @Schema(description = "상세 설명")
    String comment
) {
    public Product toEntity() {
        return Product.builder()
            .name(name)
            .price(Money.of(price))
            .comment(comment)
            .build();
    }
}

