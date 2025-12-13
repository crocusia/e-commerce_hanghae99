package com.example.ecommerce.external.dataplatform;

/**
 * 데이터 플랫폼 연동 중 발생하는 예외
 */
public class DataPlatformException extends RuntimeException {

    public DataPlatformException(String message) {
        super(message);
    }

    public DataPlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
