package com.lanyan.aidrama.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.entity.Task;
import com.lanyan.aidrama.mapper.TaskMapper;
import com.lanyan.aidrama.module.task.dto.BatchTaskStatusRequest;
import com.lanyan.aidrama.module.task.dto.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一任务服务 (系分 v1.2 第 7.5 节)
 * 所有异步操作都通过 task 表管理，前端按 taskId 或 batchId 轮询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;

    /**
     * 查询任务状态
     */
    public TaskVO getTaskStatus(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return toVO(task);
    }

    /**
     * 批量任务状态查询（支持 taskIds 或 batchId）
     */
    public List<TaskVO> batchStatus(BatchTaskStatusRequest req) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (req.getBatchId() != null && !req.getBatchId().isBlank()) {
            wrapper.eq(Task::getBatchId, req.getBatchId());
        } else if (req.getTaskIds() != null && !req.getTaskIds().isEmpty()) {
            wrapper.in(Task::getId, req.getTaskIds());
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        return taskMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 查询待轮询的任务（status=1 且 nextPollTime <= now）
     */
    public List<Task> getPollableTasks() {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, 1)
               .le(Task::getNextPollTime, java.time.LocalDateTime.now())
               .orderByAsc(Task::getNextPollTime)
               .last("LIMIT 50");
        return taskMapper.selectList(wrapper);
    }

    /**
     * 更新轮询时间（指数退避）
     */
    public void updateNextPollTime(Task task) {
        if (task.getPollCount() == null) task.setPollCount(0);
        if (task.getPollCount() >= 30) {
            log.warn("任务轮询超时, taskId: {}", task.getId());
            task.setStatus(3);
            task.setErrorMsg("任务轮询超时");
            task.setNextPollTime(null);
            taskMapper.updateById(task);
            return;
        }

        // 指数退避：10s -> 20s -> 40s -> 60s（封顶）
        long delaySeconds;
        int pollCount = task.getPollCount();
        if (pollCount == 0) delaySeconds = 10;
        else if (pollCount == 1) delaySeconds = 20;
        else if (pollCount == 2) delaySeconds = 40;
        else delaySeconds = 60;

        task.setNextPollTime(java.time.LocalDateTime.now().plusSeconds(delaySeconds));
        task.setPollCount(pollCount + 1);
        task.setLastPollTime(java.time.LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private TaskVO toVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setResultData(task.getResultData());
        vo.setResultUrl(task.getResultUrl());
        vo.setBatchId(task.getBatchId());
        return vo;
    }
}
