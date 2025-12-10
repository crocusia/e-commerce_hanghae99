package com.example.ecommerce.common.aop;

import com.example.ecommerce.common.exception.LockTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(1)
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object handleDistributedLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = generateKey(joinPoint, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);

        boolean lockAcquired = false;
        try {
            log.debug("Attempting to acquire distributed lock for key: {} with waitTime: {}, leaseTime: {} {}",
                lockKey, distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());

            lockAcquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
            );

            if (!lockAcquired) {
                log.error("Failed to acquire distributed lock for key: {} within {} {}",
                    lockKey, distributedLock.waitTime(), distributedLock.timeUnit());
                throw new LockTimeoutException("분산락 획득에 실패했습니다. key: " + lockKey);
            }

            log.debug("Distributed lock acquired for key: {}", lockKey);

            return joinPoint.proceed();

        } finally {
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Distributed lock released for key: {}", lockKey);
            }
        }
    }

    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = nameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
