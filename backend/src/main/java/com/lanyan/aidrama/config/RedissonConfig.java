package com.lanyan.aidrama.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁配置 (系分 5.3 / 7.2)
 * v1.2 新增: 使用 Redisson RLock 替代原生 RedisTemplate setIfAbsent
 * 自带看门狗自动续期 + 安全释放（Lua 脚本校验归属）
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Redisson 客户端 Bean
     * 用于分布式锁（workflow:lock:{projectId}），带看门狗续期机制
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;

        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionPoolSize(64)           // 连接池大小（系分 7.2）
                .setConnectionMinimumIdleSize(10)     // 最小空闲连接
                .setIdleConnectionTimeout(10000)      // 空闲连接超时 10s
                .setConnectTimeout(3000);             // 连接超时 3s

        return Redisson.create(config);
    }
}
