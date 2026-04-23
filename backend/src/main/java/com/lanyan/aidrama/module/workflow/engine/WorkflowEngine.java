package com.lanyan.aidrama.module.workflow.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyan.aidrama.entity.Episode;
import com.lanyan.aidrama.entity.Project;
import com.lanyan.aidrama.entity.WorkflowTask;
import com.lanyan.aidrama.mapper.EpisodeMapper;
import com.lanyan.aidrama.mapper.ProjectMapper;
import com.lanyan.aidrama.mapper.WorkflowTaskMapper;
import com.lanyan.aidrama.module.workflow.dto.NodeResult;
import com.lanyan.aidrama.module.workflow.dto.StepConfig;
import com.lanyan.aidrama.module.workflow.executor.NodeExecutor;
import com.lanyan.aidrama.module.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 工作流引擎 (系分 5.1 + 5.4)
 * 状态机驱动的线性执行逻辑，支持断点续跑、审核节点、停止标记
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private final ProjectMapper projectMapper;
    private final WorkflowTaskMapper workflowTaskMapper;
    private final EpisodeMapper episodeMapper;
    private final RedissonClient redissonClient;
    private final Map<String, NodeExecutor> executorMap;
    private final ObjectMapper objectMapper;

    /**
     * 工作流主入口：解析 workflow_config，按 step_order 遍历执行
     */
    public void execute(Long projectId) {
        log.info("工作流引擎启动, projectId: {}", projectId);

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            log.error("项目不存在, projectId: {}", projectId);
            releaseLock(projectId);
            return;
        }

        // 解析流程配置
        List<StepConfig> steps;
        try {
            steps = objectMapper.readValue(project.getWorkflowConfig(), new TypeReference<List<StepConfig>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析 workflow_config 失败, projectId: {}", projectId, e);
            releaseLock(projectId);
            return;
        }

        // 查询当前项目下的分集列表
        List<Episode> episodes = episodeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Episode>()
                        .eq(Episode::getProjectId, projectId)
                        .orderByAsc(Episode::getSortOrder));

        int episodeIndex = 0;

        // 按步骤顺序遍历
        for (StepConfig step : steps) {
            // 2a. 检查 step.enabled → false 则跳过，标记成功
            if (!step.isEnabled()) {
                log.info("步骤已禁用，跳过, stepType: {}", step.getStepType());
                createOrUpdateTask(projectId, step, null, 2, null, "skipped");
                continue;
            }

            // 按分集逐一执行该步骤
            for (Episode episode : episodes) {
                // 2b. 检查 Redis stop 标记
                String stopKey = "workflow:stop:" + projectId;
                RLock stopLock = redissonClient.getLock(stopKey);
                if (stopLock.isLocked()) {
                    log.info("检测到停止标记，终止执行, projectId: {}", projectId);
                    releaseLock(projectId);
                    return;
                }

                // 2c. 创建 workflow_task (status=1 执行中)
                createOrUpdateTask(projectId, step, episode.getId(), 1, null, "submit");
                log.info("执行步骤, stepType: {}, episode: {}", step.getStepType(), episode.getTitle());

                // 获取对应 NodeExecutor
                NodeExecutor executor = executorMap.get(step.getStepType());
                if (executor == null) {
                    log.error("未找到 NodeExecutor, stepType: {}", step.getStepType());
                    createOrUpdateTask(projectId, step, episode.getId(), 3, "不支持的步骤类型", "submit");
                    releaseLock(projectId);
                    return;
                }

                // 2d. 调用 NodeExecutor 执行具体逻辑
                NodeResult result;
                try {
                    result = executor.execute(projectId, episode.getId(), step);
                } catch (Exception e) {
                    log.error("NodeExecutor 执行异常, stepType: {}", step.getStepType(), e);
                    createOrUpdateTask(projectId, step, episode.getId(), 3, e.getMessage(), "submit");
                    releaseLock(projectId);
                    return;
                }

                // 2e. 根据执行结果处理
                switch (result.getStatus()) {
                    case "success" -> {
                        // 执行成功
                        createOrUpdateTask(projectId, step, episode.getId(), 2, result.getOutputData(), null);
                        log.info("步骤执行成功, stepType: {}", step.getStepType());

                        // 如果是最后一个分集且步骤需要审核，进入待审核状态
                        if (step.isReview() && episodeIndex == episodes.size() - 1) {
                            createOrUpdateTask(projectId, step, episode.getId(), 4, result.getOutputData(), "submit");
                            log.info("进入审核节点, stepType: {}, projectId: {}", step.getStepType(), projectId);
                            // 释放当前线程，锁由看门狗续期
                            return;
                        }
                    }
                    case "fail" -> {
                        // 执行失败
                        createOrUpdateTask(projectId, step, episode.getId(), 3, result.getErrorMsg(), result.getSubStep());
                        log.error("步骤执行失败, stepType: {}, error: {}", step.getStepType(), result.getErrorMsg());
                        releaseLock(projectId);
                        return;
                    }
                    case "pending" -> {
                        // 待审核
                        createOrUpdateTask(projectId, step, episode.getId(), 4, result.getOutputData(), result.getSubStep());
                        log.info("步骤待审核, stepType: {}", step.getStepType());
                        return;
                    }
                }
            }

            episodeIndex++;
        }

        // 3. 所有步骤完成
        project.setStatus(2); // 已完成
        project.setExecutionLock(0);
        projectMapper.updateById(project);
        releaseLock(projectId);
        log.info("工作流全部执行完成, projectId: {}", projectId);
    }

    /**
     * 审核通过后继续执行下一步
     */
    public void continueAfterReview(Long projectId) {
        log.info("审核通过，继续执行下一步, projectId: {}", projectId);

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }

        // 重新获取锁
        String lockKey = "workflow:lock:" + projectId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(5, 2, TimeUnit.HOURS);
            if (!acquired) {
                log.error("审核通过后获取锁失败, projectId: {}", projectId);
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        projectMapper.updateExecutionLock(projectId, 1);

        // 找到下一个未成功执行的步骤
        List<WorkflowTask> tasks = workflowTaskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowTask>()
                        .eq(WorkflowTask::getProjectId, projectId)
                        .orderByAsc(WorkflowTask::getStepOrder));

        // 找到最后一个状态 != 2(成功) 的任务
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (tasks.get(i).getStatus() != 2) {
                // 从这里恢复
                recoverFromTask(project, tasks.get(i));
                return;
            }
        }

        // 如果所有步骤都成功，完成
        project.setStatus(2);
        project.setExecutionLock(0);
        projectMapper.updateById(project);
        lock.unlock();
        log.info("所有步骤已完成, projectId: {}", projectId);
    }

    /**
     * 从指定任务恢复执行
     */
    private void recoverFromTask(Project project, WorkflowTask task) {
        String subStep = task.getSubStep();
        log.info("恢复执行, projectId: {}, taskType: {}, subStep: {}",
                project.getId(), task.getStepType(), subStep);

        if (subStep == null || subStep.isBlank()) {
            // 无 sub_step 记录，从头重新执行
            executeStep(project, task.getStepType(), task.getEpisodeId());
        } else {
            // 根据 sub_step 决定恢复策略
            executeStep(project, task.getStepType(), task.getEpisodeId());
        }
    }

    /**
     * 执行单个步骤
     */
    private void executeStep(Project project, String stepType, Long episodeId) {
        NodeExecutor executor = executorMap.get(stepType);
        if (executor == null) {
            log.error("未找到 NodeExecutor, stepType: {}", stepType);
            return;
        }

        // TODO: 解析 workflow_config 获取 step config
        StepConfig config = new StepConfig();
        config.setStepType(stepType);

        NodeResult result = executor.execute(project.getId(), episodeId, config);
        if ("success".equals(result.getStatus())) {
            log.info("恢复步骤执行成功, stepType: {}", stepType);
            // 继续后续步骤
            // 简化处理：实际应从 workflow_config 找到下一个步骤
        }
    }

    /**
     * 创建或更新 workflow_task
     */
    private void createOrUpdateTask(Long projectId, StepConfig step, Long episodeId,
                                    int status, String outputData, String subStep) {
        WorkflowTask task = new WorkflowTask();
        task.setProjectId(projectId);
        task.setEpisodeId(episodeId);
        task.setStepType(step.getStepType());
        task.setStepOrder(step.getStepOrder());
        task.setStatus(status);
        task.setSubStep(subStep);
        task.setOutputData(outputData);
        workflowTaskMapper.insert(task);
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(Long projectId) {
        String lockKey = "workflow:lock:" + projectId;
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
        projectMapper.updateExecutionLock(projectId, 0);
    }
}
