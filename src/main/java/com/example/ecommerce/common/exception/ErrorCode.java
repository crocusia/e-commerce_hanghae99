package com.example.ecommerce.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE("PRODUCT_001", HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR("PRODUCT_002", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED("PRODUCT_003", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),

    //product
    PRODUCT_NOT_FOUND("PRODUCT_001", HttpStatus.NOT_FOUND, "조회된 상품이 없습니다."),
    PRODUCT_OUT_OF_STOCK("PRODUCT_002", HttpStatus.CONFLICT, "상품의 재고가 부족합니다."),
    PRODUCT_ALREADY_EXISTS("PRODUCT_003", HttpStatus.CONFLICT, "이미 존재하는 상품입니다."),
    INVALID_PRODUCT_STATUS("PRODUCT_004", HttpStatus.BAD_REQUEST, "유효하지 않은 상품 상태입니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;
}
