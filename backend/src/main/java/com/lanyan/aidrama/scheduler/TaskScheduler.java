package com.lanyan.aidrama.scheduler;

import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.entity.Task;
import com.lanyan.aidrama.entity.Episode;
import com.lanyan.aidrama.mapper.TaskMapper;
import com.lanyan.aidrama.mapper.ShotMapper;
import com.lanyan.aidrama.mapper.EpisodeMapper;
import com.lanyan.aidrama.module.aitask.client.DoubaoClient;
import com.lanyan.aidrama.module.aitask.client.VideoGenClient;
import com.lanyan.aidrama.module.task.service.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 统一任务调度器 (系分 v1.2)
 * 基于新 task 表轮询第三方 AI 任务结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskScheduler {

    private final TaskMapper taskMapper;
    private final ShotMapper shotMapper;
    private final EpisodeMapper episodeMapper;
    private final DoubaoClient doubaoClient;
    private final VideoGenClient videoGenClient;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    /**
     * 轮询处理中的任务（status=1），每 5 秒执行一次
     */
    @Scheduled(fixedDelay = 5000, scheduler = "schedulerExecutor")
    public void pollTasks() {
        List<Task> tasks = taskService.getPollableTasks();
        if (tasks.isEmpty()) {
            return;
        }

        log.info("定时轮询: 发现 {} 个到期任务", tasks.size());
        for (Task task : tasks) {
            try {
                pollSingleTask(task);
            } catch (Exception e) {
                log.error("轮询任务失败, taskId: {}", task.getId(), e);
                taskService.updateNextPollTime(task);
            }
        }
    }

    /**
     * 轮询单个任务结果
     */
    private void pollSingleTask(Task task) {
        if (task.getProviderTaskId() == null) {
            // 还没有拿到第三方任务ID，等待下次轮询
            taskService.updateNextPollTime(task);
            return;
        }

        if ("video_gen".equals(task.getType())) {
            pollVideoTask(task);
        } else if ("script_analyze".equals(task.getType()) ||
                   "shot_split".equals(task.getType()) ||
                   "asset_extract".equals(task.getType()) ||
                   "prompt_gen".equals(task.getType()) ||
                   "image_gen".equals(task.getType())) {
            // 同步任务已完成，不再需要轮询
            task.setNextPollTime(null);
            taskMapper.updateById(task);
        }
    }

    /**
     * 轮询视频生成任务
     */
    private void pollVideoTask(Task task) {
        Map<String, String> result = videoGenClient.queryTaskStatus(task.getProviderTaskId());
        String status = result.get("status");

        switch (status) {
            case "succeeded" -> {
                String videoUrl = result.get("videoUrl");
                task.setStatus(2);
                task.setResultUrl(videoUrl);
                task.setNextPollTime(null);
                taskMapper.updateById(task);

                // 更新分镜视频状态
                if (task.getShotId() != null) {
                    Shot shot = shotMapper.selectById(task.getShotId());
                    if (shot != null) {
                        shot.setGeneratedVideoUrl(videoUrl);
                        shot.setVideoStatus("success");
                        shotMapper.updateById(shot);
                    }
                }
                log.info("视频生成完成, taskId: {}, url: {}", task.getId(), videoUrl);
            }
            case "failed" -> {
                task.setStatus(3);
                task.setErrorMsg("视频生成失败: " + result.get("error"));
                task.setNextPollTime(null);
                taskMapper.updateById(task);

                if (task.getShotId() != null) {
                    Shot shot = shotMapper.selectById(task.getShotId());
                    if (shot != null) {
                        shot.setVideoStatus("failed");
                        shot.setErrorMsg("视频生成失败: " + result.get("error"));
                        shotMapper.updateById(shot);
                    }
                }
            }
            default -> {
                // 仍在处理中，继续轮询
                taskService.updateNextPollTime(task);
            }
        }
    }

    /**
     * 每 10min 清理过期的缓存
     */
    @Scheduled(fixedDelay = 600000, scheduler = "schedulerExecutor")
    public void cleanExpiredCache() {
        log.info("定时清理: 清理过期缓存");
    }
}
