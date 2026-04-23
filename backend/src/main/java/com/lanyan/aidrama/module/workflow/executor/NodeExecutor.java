package com.lanyan.aidrama.module.workflow.executor;

import com.lanyan.aidrama.module.workflow.dto.NodeResult;

/**
 * 节点执行器接口 (系分 5.2)
 * 策略模式：每种步骤类型对应一个实现类
 */
public interface NodeExecutor {

    /**
     * 返回该执行器处理的步骤类型
     * @return import / asset_extract / shot_gen / image_gen / video_gen / export
     */
    String getStepType();

    /**
     * 执行节点逻辑
     * @param projectId 项目ID
     * @param episodeId 分集ID（可为空，如 export 步骤）
     * @param config 步骤配置
     * @return 执行结果
     */
    NodeResult execute(Long projectId, Long episodeId, com.lanyan.aidrama.module.workflow.dto.StepConfig config);
}
