package com.example.ecommerce.common.exception;

public class LockInterruptedException extends CustomException {

    public LockInterruptedException() {
        super(ErrorCode.LOCK_INTERRUPTED);
    }

    public LockInterruptedException(String message) {
        super(ErrorCode.LOCK_INTERRUPTED, message);
    }

    public LockInterruptedException(Throwable cause) {
        super(ErrorCode.LOCK_INTERRUPTED, cause);
    }
}
