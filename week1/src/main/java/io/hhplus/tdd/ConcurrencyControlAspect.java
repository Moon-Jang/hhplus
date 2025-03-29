package io.hhplus.tdd;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Aspect
@Component
public class ConcurrencyControlAspect {
    private final String LOCK_PREFIX = "LOCK";
    private final ConcurrentMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

    @Around("@annotation(cc)")
    public Object handle(ProceedingJoinPoint pjp, ConcurrencyControl cc) throws Throwable {
        Object result = null;
        String key = cacheKey(pjp, cc);
        ReentrantLock lock = LOCK_MAP.computeIfAbsent(key, (k) -> new ReentrantLock(true));

        try {
            if (lock.tryLock(cc.timeout(), cc.timeUnit())) {
                try {
                    result = pjp.proceed();
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("락 획득 실패");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        }

        return result;
    }

    private String cacheKey(ProceedingJoinPoint pjp, ConcurrencyControl cc) {
        String dynamicValue = CustomSpringELParser.getDynamicValue(
                ((MethodSignature) pjp.getSignature()).getParameterNames(),
                pjp.getArgs(),
                cc.key()
            )
            .toString();

        return "%s::%s".formatted(
            LOCK_PREFIX,
            dynamicValue
        );
    }
}
