package com.lanyan.aidrama.module.aitask.service;

import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.module.aitask.dto.AiTaskVO;

/**
 * AI 任务服务接口 (系分 4.6.2)
 */
public interface AiTaskService {

    /**
     * 提交图片生成任务
     * @param shot 分镜实体
     * @return AI 任务ID
     */
    Long submitImageGenTask(Shot shot);

    /**
     * 提交视频生成任务
     * @param shot 分镜实体
     * @return AI 任务ID
     */
    Long submitVideoGenTask(Shot shot);

    /**
     * 查询 AI 任务状态
     * @param taskId 任务ID
     * @return 任务视图对象
     */
    AiTaskVO getTaskStatus(Long taskId);

    /**
     * 按分镜ID查询最新 AI 任务
     * @param shotId 分镜ID
     * @return 任务视图对象
     */
    AiTaskVO getLatestTaskByShotId(Long shotId);

    /**
     * 轮询单个 AI 任务结果（指数退避）
     * @param taskId 任务ID
     */
    void pollTaskResult(Long taskId);
}
