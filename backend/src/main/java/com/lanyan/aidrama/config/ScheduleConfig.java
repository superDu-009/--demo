package com.lanyan.aidrama.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务调度配置 (系分 6.2)
 * v1.2 新增: @Scheduled 默认单线程会阻塞后续定时任务，需独立线程池
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    /**
     * 定时任务调度线程池
     * 用于 @Scheduled 注解的定时任务（AI任务轮询、缓存清理等）
     * v1.2: 池大小=3，避免单线程阻塞
     */
    @Bean("schedulerExecutor")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 调度线程池大小
        scheduler.setPoolSize(3);
        // 线程名前缀，便于日志追踪
        scheduler.setThreadNamePrefix("scheduler-");
        return scheduler;
    }
}
