package com.lanyan.aidrama.module.workflow.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyan.aidrama.entity.Project;
import com.lanyan.aidrama.entity.WorkflowTask;
import com.lanyan.aidrama.mapper.ProjectMapper;
import com.lanyan.aidrama.mapper.WorkflowTaskMapper;
import com.lanyan.aidrama.module.workflow.service.WorkflowService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 恢复执行 Runner (系分 5.4 + 5.5)
 * 服务启动时扫描 execution_lock=1 的项目，尝试获取 Redis 锁进行断点续跑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecoveryRunner {

    private final ProjectMapper projectMapper;
    private final WorkflowTaskMapper workflowTaskMapper;
    private final RedissonClient redissonClient;
    private final WorkflowService workflowService;
    private final WorkflowEngine workflowEngine;

    /**
     * 服务启动时自动扫描并恢复执行中的流程
     */
    @PostConstruct
    public void recover() {
        log.info("RecoveryRunner: 服务启动，开始扫描待恢复的流程...");

        // 1. 查询 execution_lock=1 的项目
        List<Project> lockedProjects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getExecutionLock, 1));

        if (lockedProjects.isEmpty()) {
            log.info("RecoveryRunner: 没有需要恢复的流程");
            return;
        }

        log.info("RecoveryRunner: 发现 {} 个执行中的项目，尝试恢复...", lockedProjects.size());

        for (Project project : lockedProjects) {
            String lockKey = "workflow:lock:" + project.getId();
            RLock lock = redissonClient.getLock(lockKey);

            try {
                // 2. 尝试获取 Redis 锁（非阻塞 tryLock）
                boolean acquired = lock.tryLock(0, 2, TimeUnit.HOURS);

                if (acquired) {
                    // 3. 获取成功 → 说明原锁已丢失，执行恢复流程
                    log.info("RecoveryRunner: 获取锁成功，恢复项目, projectId: {}", project.getId());
                    recoverProject(project);
                } else {
                    // 4. 获取失败 → 说明有其他实例正在执行，跳过
                    log.info("RecoveryRunner: 锁已被占用，跳过, projectId: {}", project.getId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("RecoveryRunner: 恢复流程被中断, projectId: {}", project.getId(), e);
            } catch (Exception e) {
                log.error("RecoveryRunner: 恢复流程异常, projectId: {}", project.getId(), e);
                // 确保异常时也释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        log.info("RecoveryRunner: 恢复扫描完成");
    }

    /**
     * 恢复单个项目的执行
     */
    private void recoverProject(Project project) {
        Long projectId = project.getId();

        // 1. 查找最后一个 status=1(执行中) 的 workflow_task
        WorkflowTask runningTask = workflowTaskMapper.selectLatestRunning(projectId);

        if (runningTask == null) {
            // 无待恢复任务，可能是上次异常未清理
            log.warn("RecoveryRunner: 无 running 任务，强制解锁, projectId: {}", projectId);
            projectMapper.updateExecutionLock(projectId, 0);
            return;
        }

        // 2. 根据 sub_step 决定恢复策略
        String subStep = runningTask.getSubStep();
        log.info("RecoveryRunner: 恢复任务, projectId: {}, stepType: {}, subStep: {}",
                projectId, runningTask.getStepType(), subStep);

        if (subStep == null || subStep.isBlank()) {
            // 无 sub_step 记录 → 从头重新执行该步骤
            log.info("RecoveryRunner: 无 sub_step，重新执行该步骤");
            workflowEngine.execute(projectId);
        } else {
            // 根据 sub_step 执行恢复
            switch (subStep) {
                case "submit" -> {
                    log.info("RecoveryRunner: 提交阶段中断，重新提交");
                    workflowEngine.execute(projectId);
                }
                case "polling" -> {
                    log.info("RecoveryRunner: 轮询阶段中断，重新查询第三方 API 状态");
                    workflowEngine.execute(projectId);
                }
                case "download" -> {
                    log.info("RecoveryRunner: 下载阶段中断，重新下载");
                    workflowEngine.execute(projectId);
                }
                case "upload_tos" -> {
                    log.info("RecoveryRunner: 上传阶段中断，重新上传");
                    workflowEngine.execute(projectId);
                }
                default -> {
                    log.info("RecoveryRunner: 未知 sub_step，从头重新执行");
                    workflowEngine.execute(projectId);
                }
            }
        }
    }
}
