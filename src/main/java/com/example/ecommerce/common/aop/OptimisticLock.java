package com.example.ecommerce.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 낙관적 락을 적용하는 어노테이션
 * JPA의 @Version을 사용한 낙관적 락 충돌 발생 시 재시도를 수행합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptimisticLock {

    int maxRetries() default 3;

    long retryDelay() default 100L;

    boolean exponentialBackoff() default true;
}
