package com.example.ecommerce.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("CustomException occurred: code={}, message={}", e.getCode(), e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
            e.getCode(),
            e.getMessage()
        );

        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(errorResponse);
    }

    @ExceptionHandler(EventPublishException.class)
    public ResponseEntity<ErrorResponse> handleEventPublishException(EventPublishException e) {
        log.error("Event publish failed: code={}, message={}", e.getCode(), e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
            e.getCode(),
            "요청은 처리되었으나 일부 후속 작업에서 문제가 발생했습니다."
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    @ExceptionHandler({LockTimeoutException.class, LockInterruptedException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleLockException(CustomException e) {
        log.warn("Lock exception occurred: code={}, message={}", e.getCode(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
            e.getCode(),
            e.getMessage()
        );

        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            message != null ? message : ErrorCode.INVALID_INPUT_VALUE.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            "잘못된 요청 파라미터 타입입니다: " + e.getName()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);

        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }
}
