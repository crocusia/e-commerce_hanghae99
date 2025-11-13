package com.example.ecommerce.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PessimisticLock {

    String key();

    long timeout() default 5000L;

    long leaseTime() default 3000L;

    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
