package com.example.ecommerce.common.exception;

public class LockTimeoutException extends CustomException {

    public LockTimeoutException() {
        super(ErrorCode.LOCK_TIMEOUT);
    }

    public LockTimeoutException(String message) {
        super(ErrorCode.LOCK_TIMEOUT, message);
    }

    public LockTimeoutException(Throwable cause) {
        super(ErrorCode.LOCK_TIMEOUT, cause);
    }
}
