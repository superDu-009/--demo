package com.lanyan.aidrama.scheduler;

import com.lanyan.aidrama.entity.AiTask;
import com.lanyan.aidrama.mapper.AiTaskMapper;
import com.lanyan.aidrama.module.aitask.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 任务调度器 (系分 6.3 + 8.1)
 * 负责指数退避轮询 AI 任务结果 + 定时清理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskScheduler {

    private final AiTaskMapper aiTaskMapper;
    private final AiTaskService aiTaskService;

    /**
     * 指数退避轮询处理中的 AI 任务结果
     * 每 5 秒执行一次，查询到期任务触发轮询
     * SELECT * FROM ai_task WHERE status=1 AND next_poll_time <= NOW() ORDER BY next_poll_time LIMIT 50
     */
    @Scheduled(fixedDelay = 5000, scheduler = "schedulerExecutor")
    public void pollAiTaskResults() {
        List<AiTask> tasks = aiTaskMapper.selectPollableTasks();
        if (tasks.isEmpty()) {
            return;
        }

        log.info("定时轮询: 发现 {} 个到期 AI 任务", tasks.size());
        for (AiTask task : tasks) {
            try {
                aiTaskService.pollTaskResult(task.getId());
            } catch (Exception e) {
                log.error("轮询 AI 任务失败, taskId: {}", task.getId(), e);
            }
        }
    }

    /**
     * 每 10min 清理过期的 Redis 状态缓存
     * Redis TTL 自动过期为主，此为兜底
     */
    @Scheduled(fixedDelay = 600000, scheduler = "schedulerExecutor")
    public void cleanExpiredCache() {
        log.info("定时清理: 清理过期缓存");
        // TODO: 后续接入 Redis 缓存清理逻辑
    }
}
