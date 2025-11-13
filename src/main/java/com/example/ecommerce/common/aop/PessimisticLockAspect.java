package com.example.ecommerce.common.aop;

import com.example.ecommerce.common.exception.LockInterruptedException;
import com.example.ecommerce.common.exception.LockTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PessimisticLockAspect {

    private final Map<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Around("@annotation(pessimisticLock) && args(resourceId, ..)")
    public Object handlePessimisticLock(ProceedingJoinPoint joinPoint,
        PessimisticLock pessimisticLock, long resourceId) throws Throwable {
        ReentrantLock lock = lockMap.computeIfAbsent(resourceId, key -> new ReentrantLock());
        boolean lockAcquired = false;

        try {
            log.debug("Attempting to acquire pessimistic lock for resource {} with timeout {} {}",
                resourceId, pessimisticLock.timeout(), pessimisticLock.timeUnit());

            lockAcquired = lock.tryLock(pessimisticLock.timeout(), pessimisticLock.timeUnit());

            if (!lockAcquired) {
                log.error("Failed to acquire pessimistic lock for resource {} within timeout",
                    resourceId);
                throw new LockTimeoutException("락 획득에 실패했습니다. resourceId: " + resourceId);
            }

            log.debug("Pessimistic lock acquired for resource {}", resourceId);

            // 비즈니스 로직 실행
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for resource {}", resourceId);
            throw new LockInterruptedException(e);
        } finally {
            if (lockAcquired) {
                lock.unlock();
                log.debug("Pessimistic lock released for resource {}", resourceId);
            }
        }
    }
}
