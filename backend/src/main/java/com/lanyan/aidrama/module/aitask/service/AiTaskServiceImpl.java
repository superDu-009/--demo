package com.lanyan.aidrama.module.aitask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.ShotStatus;
import com.lanyan.aidrama.entity.AiTask;
import com.lanyan.aidrama.entity.Asset;
import com.lanyan.aidrama.entity.Episode;
import com.lanyan.aidrama.entity.Scene;
import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.entity.ShotAssetRef;
import com.lanyan.aidrama.mapper.AiTaskMapper;
import com.lanyan.aidrama.mapper.AssetMapper;
import com.lanyan.aidrama.mapper.EpisodeMapper;
import com.lanyan.aidrama.mapper.SceneMapper;
import com.lanyan.aidrama.mapper.ShotAssetRefMapper;
import com.lanyan.aidrama.mapper.ShotMapper;
import com.lanyan.aidrama.module.aitask.client.ImageGenClient;
import com.lanyan.aidrama.module.aitask.client.VideoGenClient;
import com.lanyan.aidrama.module.aitask.dto.AiTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final AssetMapper assetMapper;
    private final ShotAssetRefMapper shotAssetRefMapper;
    private final ImageGenClient imageGenClient;
    private final VideoGenClient videoGenClient;

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

        try {
            // 构建参考图列表：从分镜关联的资产中取主图
            List<String> referenceImages = getReferenceImages(shot.getId());

            // 调用 Doubao 图片生成 API
            List<String> imageUrls = imageGenClient.generateImage(shot.getPromptEn(), referenceImages);

            if (imageUrls.isEmpty()) {
                task.setStatus(3); // 失败
                task.setErrorMsg("AI 生成图片为空");
                task.setNextPollTime(null);
            } else {
                // 图片生成成功，更新任务结果
                task.setStatus(2); // 成功
                task.setResultUrl(imageUrls.get(0));
                task.setProviderTaskId("doubao_img_" + task.getId());
                // 更新分镜结果
                updateShotImageResult(shot.getId(), imageUrls.get(0));
            }
        } catch (Exception e) {
            log.error("提交图片生成任务失败, shotId: {}", shot.getId(), e);
            task.setStatus(3); // 失败
            task.setErrorMsg("图片生成失败: " + e.getMessage());
            task.setNextPollTime(null);
        }

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

        try {
            // 获取参考图和首帧图片
            List<String> referenceImages = getReferenceImages(shot.getId());
            String firstFrameImage = shot.getGeneratedImageUrl();

            // 调用 Doubao 视频生成 API（异步任务模式）
            String providerTaskId = videoGenClient.submitVideoTask(shot.getPromptEn(), firstFrameImage, referenceImages);
            task.setProviderTaskId(providerTaskId);
            task.setStatus(1); // 处理中（等待轮询）
        } catch (Exception e) {
            log.error("提交视频生成任务失败, shotId: {}", shot.getId(), e);
            task.setStatus(3); // 失败
            task.setErrorMsg("视频生成失败: " + e.getMessage());
            task.setNextPollTime(null);
        }

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

        // 查询第三方 API 状态
        if (task.getProviderTaskId() == null) {
            log.warn("AI 任务无 providerTaskId, 跳过轮询, taskId: {}", taskId);
            updateNextPollTime(task);
            return;
        }

        try {
            if ("video_gen".equals(task.getTaskType())) {
                Map<String, String> result = videoGenClient.queryTaskStatus(task.getProviderTaskId());
                String status = result.get("status");

                switch (status) {
                    case "succeeded" -> {
                        String videoUrl = result.get("videoUrl");
                        task.setStatus(2); // 成功
                        task.setResultUrl(videoUrl);
                        updateShotVideoResult(task.getShotId(), videoUrl);
                        log.info("视频生成完成, taskId: {}, url: {}", taskId, videoUrl);
                    }
                    case "failed" -> {
                        task.setStatus(3); // 失败
                        task.setErrorMsg("视频生成失败: " + result.get("error"));
                        log.error("视频生成失败, taskId: {}, error: {}", taskId, result.get("error"));
                    }
                    default -> {
                        // 仍在处理中，更新下次轮询时间
                        updateNextPollTime(task);
                        return;
                    }
                }
            }
            // 图片生成是同步完成的，不需要轮询
        } catch (Exception e) {
            log.error("轮询 AI 任务异常, taskId: {}", taskId, e);
            updateNextPollTime(task);
            return;
        }

        task.setLastPollTime(LocalDateTime.now());
        aiTaskMapper.updateById(task);
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
     * 获取分镜关联资产的参考图列表
     * 取每个资产的 reference_images 第一个作为主图
     */
    private List<String> getReferenceImages(Long shotId) {
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getShotId, shotId);
        List<ShotAssetRef> refs = shotAssetRefMapper.selectList(refWrapper);

        if (refs.isEmpty()) {
            return List.of();
        }

        List<Long> assetIds = refs.stream()
                .map(ShotAssetRef::getAssetId)
                .distinct()
                .toList();

        List<Asset> assets = assetMapper.selectBatchIds(assetIds);
        return assets.stream()
                .map(Asset::getReferenceImages)
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    /**
     * 更新分镜图片生成结果
     */
    private void updateShotImageResult(Long shotId, String imageUrl) {
        Shot shot = shotMapper.selectById(shotId);
        if (shot == null) return;
        shot.setGeneratedImageUrl(imageUrl);
        shot.setStatus(ShotStatus.REVIEWING);
        shotMapper.updateById(shot);
    }

    /**
     * 更新分镜视频生成结果
     */
    private void updateShotVideoResult(Long shotId, String videoUrl) {
        Shot shot = shotMapper.selectById(shotId);
        if (shot == null) return;
        shot.setGeneratedVideoUrl(videoUrl);
        shot.setStatus(ShotStatus.COMPLETED);
        shotMapper.updateById(shot);
    }

    /**
     * 计算下次轮询时间（指数退避）
     * 策略: 10s → 20s → 40s → 60s(封顶)
     */
    private void updateNextPollTime(AiTask task) {
        long pollCount = task.getPollCount();
        long delaySeconds;
        if (pollCount == 0) {
            delaySeconds = 10;
        } else if (pollCount == 1) {
            delaySeconds = 20;
        } else if (pollCount == 2) {
            delaySeconds = 40;
        } else {
            delaySeconds = 60; // 封顶
        }
        task.setNextPollTime(LocalDateTime.now().plusSeconds(delaySeconds));
        task.setPollCount((int) (pollCount + 1));
    }

    /**
     * AI任务成功时更新分镜结果
     */
    private void updateShotResult(AiTask task) {
        if ("image_gen".equals(task.getTaskType())) {
            updateShotImageResult(task.getShotId(), task.getResultUrl());
        } else if ("video_gen".equals(task.getTaskType())) {
            updateShotVideoResult(task.getShotId(), task.getResultUrl());
        }
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
