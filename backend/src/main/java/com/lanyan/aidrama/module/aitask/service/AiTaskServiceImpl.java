package com.lanyan.aidrama.module.aitask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.ShotStatus;
import com.lanyan.aidrama.entity.AiTask;
import com.lanyan.aidrama.entity.Episode;
import com.lanyan.aidrama.entity.Scene;
import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.mapper.AiTaskMapper;
import com.lanyan.aidrama.mapper.EpisodeMapper;
import com.lanyan.aidrama.mapper.SceneMapper;
import com.lanyan.aidrama.mapper.ShotMapper;
import com.lanyan.aidrama.module.aitask.dto.AiTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI任务服务实现类 (系分 4.6.2)
 * 负责 AI 图片/视频生成的异步提交、结果轮询、状态管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskServiceImpl implements AiTaskService {

    private final AiTaskMapper aiTaskMapper;
    private final ShotMapper shotMapper;
    private final SceneMapper sceneMapper;
    private final EpisodeMapper episodeMapper;

    @Override
    @Async("aiTaskExecutor")
    public Long submitImageGenTask(Shot shot) {
        log.info("提交图片生成任务, shotId: {}", shot.getId());

        // 创建 ai_task 记录
        AiTask task = new AiTask();
        task.setProjectId(getProjectIdByShot(shot));
        task.setShotId(shot.getId());
        task.setTaskType("image_gen");
        task.setStatus(0); // 提交中
        task.setNextPollTime(LocalDateTime.now().plusSeconds(10)); // 首次轮询 10s 后
        task.setPollCount(0);
        aiTaskMapper.insert(task);

        log.info("创建 AI 任务成功, taskId: {}, type: image_gen", task.getId());

        // TODO: 后续接入真实图片生成 API
        // 当前阶段：标记为处理中，由定时轮询模拟完成
        task.setStatus(1); // 处理中
        aiTaskMapper.updateById(task);

        return task.getId();
    }

    @Override
    @Async("aiTaskExecutor")
    public Long submitVideoGenTask(Shot shot) {
        log.info("提交视频生成任务, shotId: {}", shot.getId());

        AiTask task = new AiTask();
        task.setProjectId(getProjectIdByShot(shot));
        task.setShotId(shot.getId());
        task.setTaskType("video_gen");
        task.setStatus(0); // 提交中
        task.setNextPollTime(LocalDateTime.now().plusSeconds(10));
        task.setPollCount(0);
        aiTaskMapper.insert(task);

        log.info("创建 AI 任务成功, taskId: {}, type: video_gen", task.getId());

        task.setStatus(1); // 处理中
        aiTaskMapper.updateById(task);

        return task.getId();
    }

    @Override
    public AiTaskVO getTaskStatus(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return toVO(task);
    }

    @Override
    public AiTaskVO getLatestTaskByShotId(Long shotId) {
        LambdaQueryWrapper<AiTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiTask::getShotId, shotId)
               .orderByDesc(AiTask::getId)
               .last("LIMIT 1");
        AiTask task = aiTaskMapper.selectOne(wrapper);
        if (task == null) {
            return null;
        }
        return toVO(task);
    }

    @Override
    public void pollTaskResult(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("轮询任务不存在, taskId: {}", taskId);
            return;
        }

        // 检查轮询次数上限
        if (task.getPollCount() > 30) {
            log.warn("AI任务轮询超时, taskId: {}, pollCount: {}", taskId, task.getPollCount());
            task.setStatus(3); // 失败
            task.setErrorMsg("AI任务轮询超时，超过30次轮询");
            aiTaskMapper.updateById(task);
            return;
        }

        // TODO: 后续接入真实图片/视频生成 API 查询结果
        // 当前阶段：模拟成功，更新轮询时间
        task.setStatus(2); // 成功
        task.setResultUrl("mock_result_url_" + task.getId());
        task.setLastPollTime(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        // 更新分镜结果
        updateShotResult(task);
        log.info("AI任务完成, taskId: {}", taskId);
    }

    // ============ 内部方法 ============

    /**
     * 根据分镜获取项目ID
     * 通过 scene -> episode 两级查询获取
     */
    private Long getProjectIdByShot(Shot shot) {
        Scene scene = sceneMapper.selectById(shot.getSceneId());
        if (scene == null) {
            log.warn("分镜所属分场不存在, sceneId: {}", shot.getSceneId());
            return null;
        }
        Episode episode = episodeMapper.selectById(scene.getEpisodeId());
        if (episode == null) {
            log.warn("分场所属分集不存在, episodeId: {}", scene.getEpisodeId());
            return null;
        }
        return episode.getProjectId();
    }

    /**
     * AI任务成功时更新分镜结果
     */
    private void updateShotResult(AiTask task) {
        Shot shot = shotMapper.selectById(task.getShotId());
        if (shot == null) {
            return;
        }

        if ("image_gen".equals(task.getTaskType())) {
            shot.setGeneratedImageUrl(task.getResultUrl());
            shot.setStatus(ShotStatus.REVIEWING);
        } else if ("video_gen".equals(task.getTaskType())) {
            shot.setGeneratedVideoUrl(task.getResultUrl());
            shot.setStatus(ShotStatus.REVIEWING);
        }
        shotMapper.updateById(shot);
    }

    /**
     * 实体转 VO
     */
    private AiTaskVO toVO(AiTask task) {
        AiTaskVO vo = new AiTaskVO();
        vo.setId(task.getId());
        vo.setTaskType(task.getTaskType());
        vo.setStatus(task.getStatus());
        vo.setResultUrl(task.getResultUrl());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }
}
