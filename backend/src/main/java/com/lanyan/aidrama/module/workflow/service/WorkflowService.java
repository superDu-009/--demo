package com.lanyan.aidrama.module.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.entity.*;
import com.lanyan.aidrama.mapper.*;
import com.lanyan.aidrama.module.workflow.dto.NodeResult;
import com.lanyan.aidrama.module.workflow.dto.StepConfig;
import com.lanyan.aidrama.module.workflow.dto.WorkflowStatusVO;
import com.lanyan.aidrama.module.workflow.dto.WorkflowStepStatusVO;
import com.lanyan.aidrama.module.workflow.engine.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 流程引擎服务 (系分 4.5.2 + 5.3)
 * 负责流程启动、进度查询、审核、停止、分布式锁管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final ProjectMapper projectMapper;
    private final WorkflowTaskMapper workflowTaskMapper;
    private final EpisodeMapper episodeMapper;
    private final RedissonClient redissonClient;
    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;

    /**
     * 启动工作流 (系分 5.3)
     * 获取 Redisson 分布式锁 → 更新 execution_lock → @Async 触发引擎
     */
    @Transactional
    public void startWorkflow(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 解析流程配置
        if (project.getWorkflowConfig() == null || project.getWorkflowConfig().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 尝试获取 Redisson 分布式锁
        String lockKey = "workflow:lock:" + projectId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired;
        try {
            acquired = lock.tryLock(0, 2, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        if (!acquired) {
            throw new BusinessException(ErrorCode.PROJECT_EXECUTING);
        }

        // 更新 project 表 execution_lock
        projectMapper.updateExecutionLock(projectId, 1);
        // 更新项目状态为"进行中"
        project.setStatus(1);
        projectMapper.updateById(project);

        log.info("工作流启动, projectId: {}, 分布式锁获取成功", projectId);

        // @Async 触发引擎
        executeWorkflowAsync(projectId);
    }

    /**
     * 查询流程状态 (系分 4.5.1)
     */
    public WorkflowStatusVO getWorkflowStatus(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        WorkflowStatusVO vo = new WorkflowStatusVO();
        vo.setExecutionLock(project.getExecutionLock());

        // 查询 workflow_task 获取各步骤状态
        LambdaQueryWrapper<WorkflowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTask::getProjectId, projectId)
               .orderByAsc(WorkflowTask::getStepOrder);
        List<WorkflowTask> tasks = workflowTaskMapper.selectList(wrapper);

        List<WorkflowStepStatusVO> stepStatusList = new ArrayList<>();
        String currentStep = null;
        for (WorkflowTask task : tasks) {
            WorkflowStepStatusVO stepVO = new WorkflowStepStatusVO();
            stepVO.setStepType(task.getStepType());
            stepVO.setStepOrder(task.getStepOrder());
            stepVO.setStatus(task.getStatus());
            stepVO.setErrorMsg(task.getErrorMsg());
            stepVO.setReviewComment(task.getReviewComment());
            stepVO.setProgress(calculateStepProgress(task));
            stepStatusList.add(stepVO);

            if (task.getStatus() == 1) {
                currentStep = task.getStepType();
                vo.setCurrentEpisodeId(task.getEpisodeId());
                // 查询当前分集标题
                if (task.getEpisodeId() != null) {
                    Episode ep = episodeMapper.selectById(task.getEpisodeId());
                    vo.setCurrentEpisodeTitle(ep != null ? ep.getTitle() : null);
                }
            }
        }

        vo.setCurrentStep(currentStep);
        vo.setSteps(stepStatusList);

        // 计算总分集数和进度
        long totalEpisodes = episodeMapper.selectCount(
                new LambdaQueryWrapper<Episode>().eq(Episode::getProjectId, projectId));
        vo.setTotalEpisodes((int) totalEpisodes);

        // 计算整体进度：成功步骤数 / 总步骤数
        int totalSteps = stepStatusList.size();
        long successSteps = tasks.stream().filter(t -> t.getStatus() == 2).count();
        vo.setOverallProgress(totalSteps > 0 ? (int) (successSteps * 100 / totalSteps) : 0);

        // 估算剩余时间（简化：按步骤数 × 固定值）
        long remainingSteps = tasks.stream().filter(t -> t.getStatus() == 0 || t.getStatus() == 1).count();
        vo.setEstimatedRemainingSeconds((int) (remainingSteps * 60));

        // 统计分镜数
        vo.setTotalShots(0);
        vo.setProcessedShots(0);

        return vo;
    }

    /**
     * 审核步骤 (系分 4.5.1 + 5.1)
     * approve: 标记成功，分配新线程继续
     * reject: 标记失败，停止执行，释放锁
     */
    @Async("workflowExecutor")
    public void reviewStep(Long projectId, String stepType, String action, String comment) {
        log.info("审核步骤, projectId: {}, stepType: {}, action: {}", projectId, stepType, action);

        // 查询当前待审核的任务
        LambdaQueryWrapper<WorkflowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTask::getProjectId, projectId)
               .eq(WorkflowTask::getStepType, stepType)
               .eq(WorkflowTask::getStatus, 4); // 4=待审核
        WorkflowTask task = workflowTaskMapper.selectOne(wrapper);
        if (task == null) {
            log.error("未找到待审核任务, projectId: {}, stepType: {}", projectId, stepType);
            return;
        }

        if ("approve".equals(action)) {
            // 审核通过：标记成功，继续执行下一步
            task.setStatus(2); // 成功
            task.setReviewComment(comment);
            workflowTaskMapper.updateById(task);
            log.info("审核通过, projectId: {}, stepType: {}", projectId, stepType);

            // 分配新线程继续执行
            workflowEngine.continueAfterReview(projectId);
        } else if ("reject".equals(action)) {
            // 审核打回：标记失败，停止执行
            task.setStatus(3); // 失败
            task.setErrorMsg(comment);
            task.setReviewComment(comment);
            workflowTaskMapper.updateById(task);
            log.info("审核打回, projectId: {}, stepType: {}, reason: {}", projectId, stepType, comment);

            // 释放锁
            releaseLock(projectId);
        }
    }

    /**
     * 停止工作流 (系分 4.5.1)
     * 写入 Redis stop 标记，引擎检查后终止
     */
    public void stopWorkflow(Long projectId) {
        String stopKey = "workflow:stop:" + projectId;
        RLock lock = redissonClient.getLock(stopKey);
        lock.lock(2, TimeUnit.HOURS);
        log.info("工作流停止标记已写入, projectId: {}", projectId);
    }

    /**
     * 异步执行工作流
     */
    @Async("workflowExecutor")
    public void executeWorkflowAsync(Long projectId) {
        try {
            workflowEngine.execute(projectId);
        } catch (Exception e) {
            log.error("工作流执行异常, projectId: {}", projectId, e);
            releaseLock(projectId);
        }
    }

    /**
     * 释放分布式锁 (系分 5.3)
     */
    private void releaseLock(Long projectId) {
        String lockKey = "workflow:lock:" + projectId;
        RLock lock = redissonClient.getLock(lockKey);
        // Redisson 自带 Lua 脚本校验归属，安全释放
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
        projectMapper.updateExecutionLock(projectId, 0);
        log.info("分布式锁释放, projectId: {}", projectId);
    }

    /**
     * 计算步骤内部进度
     */
    private int calculateStepProgress(WorkflowTask task) {
        // 简化实现：根据 sub_step 返回进度
        if (task.getStatus() == 2) return 100; // 成功 = 100%
        if (task.getStatus() == 3) return 0;   // 失败 = 0%
        if (task.getStatus() == 4) return 90;  // 待审核 = 90%

        // 根据 sub_step 估算
        if (task.getSubStep() != null) {
            return switch (task.getSubStep()) {
                case "submit" -> 20;
                case "polling" -> 40;
                case "download" -> 70;
                case "upload_tos" -> 85;
                default -> 10;
            };
        }
        return 0;
    }
}
