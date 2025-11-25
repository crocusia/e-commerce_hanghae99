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
    INVALID_PRODUCT_STATUS("PRODUCT_004", HttpStatus.BAD_REQUEST, "유효하지 않은 상품 상태입니다."),

    // User
    USER_NOT_FOUND("USER_001", HttpStatus.NOT_FOUND, "조회된 유저가 없습니다."),
    USER_INSUFFICIENT_BALANCE("USER_002", HttpStatus.BAD_REQUEST, "잔액이 부족합니다."),
    USER_INVALID_CHARGE_AMOUNT("USER_003", HttpStatus.BAD_REQUEST, "충전 금액은 0보다 커야 합니다."),
    USER_INVALID_BALANCE("USER_004", HttpStatus.BAD_REQUEST, "잔액은 0 이상이어야 합니다."),

    // Coupon
    COUPON_NOT_FOUND("COUPON_001", HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    COUPON_ALREADY_USED("COUPON_002",HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다."),
    COUPON_ALREADY_ISSUED("COUPON_003",HttpStatus.CONFLICT, "이미 발급된 쿠폰입니다."),
    COUPON_EXPIRED("COUPON_004", HttpStatus.GONE, "만료된 쿠폰입니다."),
    COUPON_NOT_AVAILABLE("COUPON_005", HttpStatus.CONFLICT, "사용할 수 없는 쿠폰입니다."),

    //UserCoupon
    USER_COUPON_NOT_FOUND("USER_COUPON_001", HttpStatus.NOT_FOUND, "사용자의 쿠폰을 찾을 수 없습니다."),

    // Order
    ORDER_NOT_FOUND("ORDER_001", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_COMPLETED("ORDER_002", HttpStatus.CONFLICT, "이미 완료된 주문입니다."),
    ORDER_ALREADY_CANCELLED("ORDER_003", HttpStatus.CONFLICT, "이미 취소된 주문입니다."),
    INVALID_ORDER_STATUS_APPLY_COUPON("ORDER_004",HttpStatus.BAD_REQUEST, "쿠폰을 변경할 수 없는 주문 상태입니다."),
    INVALID_ORDER_STATUS_PROCESS_PAYMENT("ORDER_005", HttpStatus.BAD_REQUEST, "결제를 진행할 수 없는 주문 상태입니다."),

    // Payment
    PAYMENT_NOT_FOUND("PAYMENT_001", HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),
    PAYMENT_FAILED("PAYMENT_002", HttpStatus.PAYMENT_REQUIRED, "결제에 실패했습니다."),
    INSUFFICIENT_BALANCE("PAYMENT_003", HttpStatus.PAYMENT_REQUIRED, "잔액이 부족합니다."),
    INVALID_PAYMENT_AMOUNT("PAYMENT_004", HttpStatus.BAD_REQUEST, "결제 금액이 유효하지 않습니다."),
    BALANCE_NOT_FOUND("PAYMENT_005", HttpStatus.NOT_FOUND, "잔액 정보를 찾을 수 없습니다."),

    // Lock
    LOCK_ACQUISITION_FAILED("LOCK_001", HttpStatus.CONFLICT, "락 획득에 실패했습니다. 잠시 후 다시 시도해주세요."),
    LOCK_TIMEOUT("LOCK_002", HttpStatus.REQUEST_TIMEOUT, "락 획득 제한 시간을 초과했습니다."),
    LOCK_INTERRUPTED("LOCK_003", HttpStatus.INTERNAL_SERVER_ERROR, "락 획득 중 인터럽트가 발생했습니다."),
    OPTIMISTIC_LOCK_FAILURE("LOCK_004", HttpStatus.CONFLICT, "락 획득에 실패했습니다. 잠시 후 다시 시도해주세요."),

    EVENT_SERIALIZATION_FAILED("EVENT_001", HttpStatus.CONFLICT, "이벤트 직렬화에 실패했습니다."),
    EVENT_PUBLISH_FAILED("EVENT_002", HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 발행에 실패했습니다."),

    // Outbox
    OUTBOX_NOT_FOUND("OUTBOX_001", HttpStatus.NOT_FOUND, "Outbox를 찾을 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
