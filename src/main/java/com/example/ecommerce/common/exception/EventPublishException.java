package com.example.ecommerce.common.exception;

public class EventPublishException extends CustomException {

    public EventPublishException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EventPublishException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EventPublishException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EventPublishException(String message, Throwable cause) {
        super(ErrorCode.EVENT_PUBLISH_FAILED, cause);
    }
}
