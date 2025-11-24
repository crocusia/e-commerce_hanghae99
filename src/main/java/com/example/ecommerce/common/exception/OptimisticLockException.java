package com.example.ecommerce.common.exception;

/**
 * 낙관적 락 충돌 예외
 * JPA의 OptimisticLockException 또는 재시도 횟수 초과 시 발생합니다.
 */
public class OptimisticLockException extends CustomException {

    public OptimisticLockException() {
        super(ErrorCode.OPTIMISTIC_LOCK_FAILURE);
    }

    public OptimisticLockException(String message) {
        super(ErrorCode.OPTIMISTIC_LOCK_FAILURE, message);
    }

    public OptimisticLockException(Throwable cause) {
        super(ErrorCode.OPTIMISTIC_LOCK_FAILURE, cause);
    }

    public OptimisticLockException(String message, Throwable cause) {
        super(ErrorCode.OPTIMISTIC_LOCK_FAILURE, cause);
    }
}
