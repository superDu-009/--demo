package com.lanyan.aidrama.aspect;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 手动熔断器 (系分 4.6.4)
 * 记录连续失败次数，连续失败 5 次 → 熔断 30s → 半开试探 1 次 → 成功则恢复
 */
public class CircuitBreaker {

    /** 熔断状态 */
    public enum State {
        /** 关闭（正常） */
        CLOSED,
        /** 打开（熔断中，拒绝请求） */
        OPEN,
        /** 半开（允许 1 个试探请求） */
        HALF_OPEN
    }

    /** 连续失败次数阈值 */
    private final int failureThreshold;

    /** 熔断持续时间（毫秒） */
    private final long openDurationMs;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong openTimestamp = new AtomicLong(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

    public CircuitBreaker() {
        this(5, 30_000);
    }

    public CircuitBreaker(int failureThreshold, long openDurationMs) {
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
    }

    /**
     * 记录一次成功
     */
    public void recordSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    /**
     * 记录一次失败，可能触发熔断
     */
    public void recordFailure() {
        int count = failureCount.incrementAndGet();
        if (count >= failureThreshold) {
            state.set(State.OPEN);
            openTimestamp.set(System.currentTimeMillis());
        }
    }

    /**
     * 检查是否允许请求通过
     * @return true=允许, false=熔断中拒绝
     */
    public boolean allowRequest() {
        State currentState = state.get();
        if (currentState == State.CLOSED) {
            return true;
        }
        if (currentState == State.OPEN) {
            // 检查是否超过熔断持续时间
            if (System.currentTimeMillis() - openTimestamp.get() >= openDurationMs) {
                // 进入半开状态
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    return true;
                }
            }
            return false;
        }
        // HALF_OPEN 状态只允许 1 个请求
        return true;
    }

    /**
     * 半开状态的试探请求成功，恢复熔断器
     */
    public void recordHalfOpenSuccess() {
        if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
            failureCount.set(0);
        }
    }

    /**
     * 半开状态的试探请求失败，重新熔断
     */
    public void recordHalfOpenFailure() {
        state.compareAndSet(State.HALF_OPEN, State.OPEN);
        openTimestamp.set(System.currentTimeMillis());
    }

    public State getState() {
        return state.get();
    }
}
