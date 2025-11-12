package com.example.ecommerce.user.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "잔액 차감 요청")
public record BalanceDeductRequest(
    @NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID")
    Long userId,

    @NotNull(message = "차감 금액은 필수입니다")
    @Min(value = 1, message = "차감 금액은 1 이상이어야 합니다")
    @Schema(description = "차감 금액")
    Long amount
) {
}