package com.lanyan.aidrama.config;

import com.lanyan.aidrama.aspect.CircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 熔断器配置 (系分 4.6.4)
 * 连续失败 5 次 → 熔断 30s → 半开试探
 */
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreaker circuitBreaker() {
        // 连续失败 5 次进入熔断，持续 30 秒
        return new CircuitBreaker(5, 30_000);
    }
}
