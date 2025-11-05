package com.example.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "조회된 상품이 없습니다."),
    PRODUCT_OUT_OF_STOCK(HttpStatus.CONFLICT, "상품의 재고가 부족합니다."),
    PRODUCT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 상품입니다."),
    INVALID_PRODUCT_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 상품 상태입니다."),

    // Cart
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."),
    INVALID_CART_QUANTITY(HttpStatus.BAD_REQUEST, "장바구니 수량이 유효하지 않습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 주문입니다."),
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 주문입니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 주문 상태입니다."),

    // Payment
    PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "결제에 실패했습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.PAYMENT_REQUIRED, "잔액이 부족합니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 유효하지 않습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // Coupon
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다."),
    COUPON_EXPIRED(HttpStatus.GONE, "만료된 쿠폰입니다."),
    COUPON_NOT_AVAILABLE(HttpStatus.CONFLICT, "사용할 수 없는 쿠폰입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}