package com.example.ecommerce.dto.common;

import com.example.ecommerce.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
    @Schema(description = "에러 코드")
    String code,

    @Schema(description = "에러 메시지")
    String message
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}