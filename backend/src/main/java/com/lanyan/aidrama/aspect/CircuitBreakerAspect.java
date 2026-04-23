package com.lanyan.aidrama.aspect;

import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.module.aitask.client.DoubaoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Doubao AI 调用熔断切面 (系分 4.6.4)
 * 拦截 DoubaoClient 所有方法，记录连续失败次数，触发熔断
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CircuitBreakerAspect {

    private final CircuitBreaker circuitBreaker;

    @Around("execution(* com.lanyan.aidrama.module.aitask.client.DoubaoClient.chat(..))")
    public Object aroundDoubaoCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查是否允许请求通过
        if (!circuitBreaker.allowRequest()) {
            log.warn("Doubao API 熔断中，拒绝请求");
            throw new BusinessException(ErrorCode.AI_TIMEOUT);
        }

        try {
            Object result = joinPoint.proceed();
            // 请求成功
            if (circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN) {
                circuitBreaker.recordHalfOpenSuccess();
            } else {
                circuitBreaker.recordSuccess();
            }
            return result;
        } catch (Exception e) {
            // 请求失败
            if (circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN) {
                circuitBreaker.recordHalfOpenFailure();
            } else {
                circuitBreaker.recordFailure();
            }
            log.error("Doubao API 调用失败，当前状态: {}", circuitBreaker.getState(), e);
            throw e;
        }
    }
}
