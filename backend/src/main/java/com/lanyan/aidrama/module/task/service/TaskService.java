package com.lanyan.aidrama.module.task.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.TaskStatus;
import com.lanyan.aidrama.entity.Episode;
import com.lanyan.aidrama.entity.Project;
import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.entity.Task;
import com.lanyan.aidrama.mapper.EpisodeMapper;
import com.lanyan.aidrama.mapper.ProjectMapper;
import com.lanyan.aidrama.mapper.ShotMapper;
import com.lanyan.aidrama.mapper.TaskMapper;
import com.lanyan.aidrama.module.storage.service.TosService;
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
    private final ProjectMapper projectMapper;
    private final EpisodeMapper episodeMapper;
    private final ShotMapper shotMapper;
    private final TosService tosService;

    /**
     * 查询任务状态
     */
    public TaskVO getTaskStatus(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        validateTaskOwnership(task);
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
                .filter(this::ownsTask)
                .map(this::toVO)
                .toList();
    }

    /**
     * 查询待轮询的任务（status=1 且 nextPollTime <= now）
     */
    public List<Task> getPollableTasks() {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, TaskStatus.PROCESSING.getCode())
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
            task.setStatus(TaskStatus.FAILED.getCode());
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

    public void markTaskPolling(Task task) {
        task.setStatus(TaskStatus.PROCESSING.getCode());
        task.setNextPollTime(java.time.LocalDateTime.now().plusSeconds(5));
        task.setLastPollTime(java.time.LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private void validateTaskOwnership(Task task) {
        if (!ownsTask(task)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean ownsTask(Task task) {
        Long projectId = resolveProjectId(task);
        if (projectId == null) {
            return false;
        }
        Project project = projectMapper.selectById(projectId);
        return project != null && project.getUserId().equals(StpUtil.getLoginIdAsLong());
    }

    private Long resolveProjectId(Task task) {
        if (task.getProjectId() != null) {
            return task.getProjectId();
        }
        if (task.getEpisodeId() != null) {
            Episode episode = episodeMapper.selectById(task.getEpisodeId());
            return episode != null ? episode.getProjectId() : null;
        }
        if (task.getShotId() != null) {
            Shot shot = shotMapper.selectById(task.getShotId());
            if (shot == null) {
                return null;
            }
            Episode episode = episodeMapper.selectById(shot.getEpisodeId());
            return episode != null ? episode.getProjectId() : null;
        }
        return null;
    }

    private TaskVO toVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setResultData(task.getResultData());
        vo.setResultUrl(tosService.buildReadableUrl(task.getResultUrl()));
        vo.setBatchId(task.getBatchId());
        return vo;
    }
}
