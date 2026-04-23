package com.lanyan.aidrama.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务与线程池配置 (系分第 6 节)
 * v1.2 变更: 拒绝策略改为 AbortPolicy
 *
 * 线程池规划:
 *   - aiTaskExecutor: AI 任务异步执行线程池
 *   - workflowExecutor: 流程引擎执行线程池
 *
 * 注意: 定时任务调度已拆分为独立的 ScheduleConfig
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 流程引擎执行线程池 (系分 6.1)
     * 用于异步执行工作流引擎
     * v1.2 参数: core=5, max=8, queue=20
     */
    @Bean("workflowExecutor")
    public ThreadPoolTaskExecutor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(8);
        // 队列容量
        executor.setQueueCapacity(20);
        // 线程名前缀，便于日志追踪
        executor.setThreadNamePrefix("workflow-");
        // v1.2 变更: 使用 AbortPolicy，队列满时直接抛异常
        // 避免 CallerRunsPolicy 导致 Tomcat 工作线程被阻塞
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

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
}
