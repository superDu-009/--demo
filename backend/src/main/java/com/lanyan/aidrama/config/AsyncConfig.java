package com.lanyan.aidrama.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务与线程池配置 (系分第 6 节)
 * v1.2 变更: 拒绝策略改为 AbortPolicy，调度器使用独立线程池
 *
 * 线程池规划:
 *   - aiTaskExecutor: AI 任务异步执行线程池
 *   - schedulerExecutor: 定时任务调度线程池
 */
@Configuration
@EnableAsync    // 启用 @Async 异步方法支持
@EnableScheduling // 启用 @Scheduled 定时任务支持
public class AsyncConfig {

    /**
     * AI 任务执行线程池
     * 用于异步执行 AI 生成任务（生图、生视频等）
     * v1.2 参数: core=10, max=20, queue=50
     */
    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(10);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量
        executor.setQueueCapacity(50);
        // 线程名前缀，便于日志追踪
        executor.setThreadNamePrefix("ai-task-");
        // v1.2 变更: 使用 AbortPolicy，队列满时直接抛异常
        // 避免 CallerRunsPolicy 导致 Tomcat 工作线程被阻塞
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 定时任务调度线程池 (系分 6.2)
     * v1.2 新增: @Scheduled 默认单线程会阻塞后续定时任务，需独立线程池
     */
    @Bean("schedulerExecutor")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 调度线程池大小
        scheduler.setPoolSize(3);
        // 线程名前缀
        scheduler.setThreadNamePrefix("scheduler-");
        return scheduler;
    }
}
