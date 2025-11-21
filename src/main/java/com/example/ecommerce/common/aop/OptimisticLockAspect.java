package com.example.ecommerce.common.aop;

import com.example.ecommerce.common.exception.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(1)
public class OptimisticLockAspect {

    @Around("@annotation(optimisticLock)")
    public Object handleOptimisticLock(ProceedingJoinPoint joinPoint, OptimisticLock optimisticLock) throws Throwable {
        String methodName = getMethodName(joinPoint);
        int maxRetries = optimisticLock.maxRetries();
        long retryDelay = optimisticLock.retryDelay();
        boolean exponentialBackoff = optimisticLock.exponentialBackoff();

        int attemptCount = 0;

        while (true) {
            attemptCount++;

            try {
                log.debug("낙관적 락 실행 시도 - method: {}, attempt: {}/{}",
                    methodName, attemptCount, maxRetries);

                // 비즈니스 로직 실행
                Object result = joinPoint.proceed();

                if (attemptCount > 1) {
                    log.info("낙관적 락 재시도 성공 - method: {}, attempts: {}",
                        methodName, attemptCount);
                }

                return result;

            } catch (ObjectOptimisticLockingFailureException |
                     jakarta.persistence.OptimisticLockException e) {

                if (attemptCount >= maxRetries) {
                    log.error("낙관적 락 최대 재시도 횟수 초과 - method: {}, maxRetries: {}",
                        methodName, maxRetries);
                    throw new OptimisticLockException(
                        String.format("동시성 충돌로 인해 작업을 완료할 수 없습니다. (최대 재시도 횟수: %d)", maxRetries),
                        e
                    );
                }

                long waitTime = calculateWaitTime(retryDelay, attemptCount, exponentialBackoff);

                log.warn("낙관적 락 충돌 발생 - method: {}, attempt: {}/{}, retrying after {}ms",
                    methodName, attemptCount, maxRetries, waitTime);

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("낙관적 락 재시도 중 인터럽트 발생 - method: {}", methodName);
                    throw new OptimisticLockException("재시도 중 인터럽트가 발생했습니다.", ie);
                }
            }
        }
    }

    private long calculateWaitTime(long baseDelay, int attemptCount, boolean exponentialBackoff) {
        if (!exponentialBackoff) {
            return baseDelay;
        }

        return baseDelay * (long) Math.pow(2, attemptCount - 1);
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
