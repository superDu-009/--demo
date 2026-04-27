package com.lanyan.aidrama.module.content.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.entity.*;
import com.lanyan.aidrama.mapper.*;
import com.lanyan.aidrama.module.aitask.client.DoubaoClient;
import com.lanyan.aidrama.module.aitask.client.ImageGenClient;
import com.lanyan.aidrama.module.aitask.client.VideoGenClient;
import com.lanyan.aidrama.module.content.dto.*;
import com.lanyan.aidrama.module.storage.service.TosService;
import com.lanyan.aidrama.module.task.service.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 内容服务实现类 (系分 v1.2 第 7.3 节)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final EpisodeMapper episodeMapper;
    private final ShotMapper shotMapper;
    private final ShotAssetRefMapper shotAssetRefMapper;
    private final AssetMapper assetMapper;
    private final ProjectMapper projectMapper;
    private final PromptConfigMapper promptConfigMapper;
    private final TaskMapper taskMapper;
    private final DoubaoClient doubaoClient;
    private final ImageGenClient imageGenClient;
    private final VideoGenClient videoGenClient;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;
    private final TosService tosService;

    // ==================== 分集相关 ====================

    @Override
    public List<EpisodeVO> listEpisodes(Long projectId) {
        getProjectById(projectId);
        LambdaQueryWrapper<Episode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Episode::getProjectId, projectId)
               .orderByAsc(Episode::getSortOrder);

        return episodeMapper.selectList(wrapper).stream()
                .map(this::toEpisodeVO)
                .toList();
    }

    @Override
    public Long analyzeScript(Long projectId) {
        Project project = getProjectById(projectId);
        if (project.getNovelTosPath() == null || project.getNovelTosPath().isBlank()) {
            throw new BusinessException(40900, "请先上传小说文件");
        }

        // 创建 task 记录
        Task task = new Task();
        task.setType("script_analyze");
        task.setProjectId(projectId);
        task.setStatus(0);
        task.setPollCount(0);
        taskMapper.insert(task);

        // 异步执行
        executeScriptAnalyzeAsync(task.getId(), projectId);

        return task.getId();
    }

    @Override
    public ScriptAnalyzeStatusVO getAnalyzeStatus(Long projectId) {
        getProjectById(projectId);
        // 查找该项目最新的剧本分析任务
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getProjectId, projectId)
               .eq(Task::getType, "script_analyze")
               .orderByDesc(Task::getId)
               .last("LIMIT 1");
        Task task = taskMapper.selectOne(wrapper);

        ScriptAnalyzeStatusVO vo = new ScriptAnalyzeStatusVO();
        if (task == null) {
            vo.setParseStatus("pending");
            return vo;
        }

        vo.setTaskId(task.getId());
        // 根据 task 状态映射到解析状态
        vo.setParseStatus(switch (task.getStatus()) {
            case 0 -> "analyzing";
            case 1 -> "analyzing";
            case 2 -> "success";
            case 3 -> "failed";
            default -> "pending";
        });
        vo.setParseError(task.getErrorMsg());
        return vo;
    }

    @Async("aiTaskExecutor")
    public void executeScriptAnalyzeAsync(Long taskId, Long projectId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;

        try {
            // 更新为处理中
            task.setStatus(1);
            taskMapper.updateById(task);

            Project project = getProjectById(projectId);
            String promptText = buildSystemPrompt("script_split", "script_analyze_response.json");
            String novelText = tosService.readFileAsText(project.getNovelTosPath());

            // 调用 AI
            String aiResult = doubaoClient.chat(promptText, novelText);
            replaceEpisodesFromAiResult(projectId, aiResult);

            // 更新 task 状态为成功
            task.setStatus(2);
            task.setResultData(aiResult);
            taskMapper.updateById(task);

        } catch (Exception e) {
            log.error("剧本分析失败, projectId: {}", projectId, e);
            task.setStatus(3);
            task.setErrorMsg(e.getMessage());
            taskMapper.updateById(task);
        }
    }

    @Override
    public Long createEpisode(Long projectId, EpisodeCreateRequest req) {
        getProjectById(projectId);

        // 自动递增 sort_order
        LambdaQueryWrapper<Episode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Episode::getProjectId, projectId)
               .orderByDesc(Episode::getSortOrder)
               .last("LIMIT 1");
        Episode maxEpisode = episodeMapper.selectOne(wrapper);
        int sortOrder = (maxEpisode != null) ? maxEpisode.getSortOrder() + 1 : 0;

        Episode episode = new Episode();
        episode.setProjectId(projectId);
        episode.setTitle(req.getTitle());
        episode.setContent(req.getContent());
        episode.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : sortOrder);
        episode.setParseStatus("success");

        episodeMapper.insert(episode);
        log.info("创建分集成功, episodeId: {}", episode.getId());
        return episode.getId();
    }

    @Override
    public void updateEpisode(Long id, EpisodeUpdateRequest req) {
        Episode episode = getOwnedEpisode(id);

        if (req.getTitle() != null) episode.setTitle(req.getTitle());
        if (req.getContent() != null) episode.setContent(req.getContent());
        if (req.getSortOrder() != null) episode.setSortOrder(req.getSortOrder());

        episodeMapper.updateById(episode);
        log.info("更新分集成功, episodeId: {}", id);
    }

    @Override
    @Transactional
    public void deleteEpisode(Long id) {
        Episode episode = getOwnedEpisode(id);

        // 级联删除分镜
        LambdaQueryWrapper<Shot> shotWrapper = new LambdaQueryWrapper<>();
        shotWrapper.eq(Shot::getEpisodeId, id).select(Shot::getId);
        List<Long> shotIds = shotMapper.selectList(shotWrapper).stream()
                .map(Shot::getId).toList();

        if (!shotIds.isEmpty()) {
            LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
            refWrapper.in(ShotAssetRef::getShotId, shotIds);
            shotAssetRefMapper.delete(refWrapper);
            shotMapper.delete(new LambdaQueryWrapper<Shot>().in(Shot::getId, shotIds));
        }

        episodeMapper.deleteById(id);
        log.info("删除分集成功, episodeId: {}", id);
    }

    // ==================== 分镜相关 ====================

    @Override
    public List<ShotVO> listShots(Long episodeId, String promptStatus, String imageStatus, String videoStatus) {
        LambdaQueryWrapper<Shot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shot::getEpisodeId, episodeId)
               .eq(promptStatus != null, Shot::getPromptStatus, promptStatus)
               .eq(imageStatus != null, Shot::getImageStatus, imageStatus)
               .eq(videoStatus != null, Shot::getVideoStatus, videoStatus)
               .orderByAsc(Shot::getSortOrder);

        List<Shot> shots = shotMapper.selectList(wrapper);
        if (shots.isEmpty()) return List.of();

        // 批量查询资产关联
        List<Long> shotIds = shots.stream().map(Shot::getId).toList();
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.in(ShotAssetRef::getShotId, shotIds);
        List<ShotAssetRef> refs = shotAssetRefMapper.selectList(refWrapper);

        Map<Long, Asset> assetMap = !refs.isEmpty()
                ? assetMapper.selectBatchIds(refs.stream().map(ShotAssetRef::getAssetId).distinct().toList()).stream()
                    .collect(Collectors.toMap(Asset::getId, a -> a))
                : new java.util.HashMap<>();

        java.util.Map<Long, List<ShotAssetRef>> refsByShot = refs.stream()
                .collect(Collectors.groupingBy(ShotAssetRef::getShotId));

        return shots.stream()
                .map(shot -> toShotVO(shot, refsByShot, assetMap))
                .toList();
    }

    @Override
    public Long splitShots(Long episodeId, Integer duration) {
        Episode episode = getOwnedEpisode(episodeId);
        if (episode.getContent() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Task task = new Task();
        task.setType("shot_split");
        task.setProjectId(episode.getProjectId());
        task.setEpisodeId(episodeId);
        task.setStatus(0);
        task.setPollCount(0);
        taskMapper.insert(task);

        executeShotSplitAsync(task.getId(), episodeId, duration != null ? duration : 10);
        return task.getId();
    }

    @Async("aiTaskExecutor")
    public void executeShotSplitAsync(Long taskId, Long episodeId, Integer duration) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;

        try {
            task.setStatus(1);
            taskMapper.updateById(task);

            // 获取 prompt 并替换时长变量
            String promptText = buildSystemPrompt("shot_split", "shot_split_response.json")
                    .replace("{duration}", String.valueOf(duration));

            Episode episode = episodeMapper.selectById(episodeId);

            // 调用 AI 拆分
            String aiResult = doubaoClient.chat(promptText, episode.getContent());

            // 解析 AI 返回的 JSON，写入 shot 表
            parseAndCreateShots(episodeId, aiResult);

            task.setStatus(2);
            task.setResultData(aiResult);
            taskMapper.updateById(task);

        } catch (Exception e) {
            log.error("分镜拆分失败, episodeId: {}", episodeId, e);
            task.setStatus(3);
            task.setErrorMsg(e.getMessage());
            taskMapper.updateById(task);
        }
    }

    @Override
    public Long createShot(Long episodeId, ShotCreateRequest req) {
        Episode episode = getOwnedEpisode(episodeId);

        LambdaQueryWrapper<Shot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shot::getEpisodeId, episodeId)
               .orderByDesc(Shot::getSortOrder)
               .last("LIMIT 1");
        Shot maxShot = shotMapper.selectOne(wrapper);
        int sortOrder = (maxShot != null) ? maxShot.getSortOrder() + 1 : 0;

        Shot shot = new Shot();
        shot.setEpisodeId(episodeId);
        shot.setPrompt(req.getPrompt());
        shot.setDuration(req.getDuration());
        shot.setSceneType(req.getSceneType());
        shot.setCameraMove(req.getCameraMove());
        shot.setLines(req.getLines() != null ? toJsonString(req.getLines()) : null);
        shot.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : sortOrder);
        shot.setFollowLast(req.getFollowLast() != null ? req.getFollowLast() : 1);
        shot.setPromptStatus("pending");
        shot.setImageStatus("pending");
        shot.setVideoStatus("pending");

        shotMapper.insert(shot);

        // 绑定资产
        if (req.getAssetIds() != null && !req.getAssetIds().isEmpty()) {
            bindAssetsToShot(shot.getId(), req.getAssetIds());
        }

        log.info("创建分镜成功, shotId: {}", shot.getId());
        return shot.getId();
    }

    @Override
    public void updateShot(Long id, ShotUpdateRequest req) {
        Shot shot = getOwnedShot(id);

        if (req.getPrompt() != null) shot.setPrompt(req.getPrompt());
        if (req.getPromptEn() != null) shot.setPromptEn(req.getPromptEn());
        if (req.getSortOrder() != null) shot.setSortOrder(req.getSortOrder());
        if (req.getDuration() != null) shot.setDuration(req.getDuration());
        if (req.getSceneType() != null) shot.setSceneType(req.getSceneType());
        if (req.getCameraMove() != null) shot.setCameraMove(req.getCameraMove());
        if (req.getLines() != null) shot.setLines(toJsonString(req.getLines()));
        if (req.getFollowLast() != null) shot.setFollowLast(req.getFollowLast());

        shotMapper.updateById(shot);

        // 更新资产绑定
        if (req.getAssetIds() != null) {
            // 先解绑旧的
            LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
            refWrapper.eq(ShotAssetRef::getShotId, id);
            shotAssetRefMapper.delete(refWrapper);
            // 再绑定新的
            bindAssetsToShot(id, req.getAssetIds());
        }

        log.info("更新分镜成功, shotId: {}", id);
    }

    @Override
    public void deleteShot(Long id) {
        Shot shot = getOwnedShot(id);

        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getShotId, id);
        shotAssetRefMapper.delete(refWrapper);

        shotMapper.deleteById(id);
        log.info("删除分镜成功, shotId: {}", id);
    }

    @Override
    public void sortShot(Long id, Integer sortOrder) {
        Shot shot = getOwnedShot(id);
        shot.setSortOrder(sortOrder);
        shotMapper.updateById(shot);
        log.info("分镜排序成功, shotId: {}, sortOrder: {}", id, sortOrder);
    }

    @Override
    public void saveDraft(Long id, String draftContent) {
        Shot shot = getOwnedShot(id);
        shot.setDraftContent(draftContent);
        shotMapper.updateById(shot);
        log.info("保存分镜草稿成功, shotId: {}", id);
    }

    @Override
    public Long generatePrompt(Long id) {
        Shot shot = getOwnedShot(id);

        Task task = new Task();
        task.setType("prompt_gen");
        task.setShotId(id);
        // 通过 episode 找 projectId
        Episode episode = episodeMapper.selectById(shot.getEpisodeId());
        if (episode != null) task.setProjectId(episode.getProjectId());
        task.setStatus(0);
        task.setPollCount(0);
        taskMapper.insert(task);

        executePromptGenAsync(task.getId(), id);
        return task.getId();
    }

    @Async("aiTaskExecutor")
    public void executePromptGenAsync(Long taskId, Long shotId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;

        try {
            task.setStatus(1);
            taskMapper.updateById(task);

            Shot shot = shotMapper.selectById(shotId);
            shot.setPromptStatus("generating");
            shotMapper.updateById(shot);
            String promptText = getPromptText("prompt_gen");

            // 调用 AI 生成英文提示词
            String promptEn = doubaoClient.chat(promptText, shot.getPrompt());

            // 翻译 prompt
            shot.setPromptEn(promptEn);
            shot.setPromptStatus("success");
            shotMapper.updateById(shot);

            task.setStatus(2);
            task.setResultData(promptEn);
            taskMapper.updateById(task);

        } catch (Exception e) {
            log.error("提示词生成失败, shotId: {}", shotId, e);
            task.setStatus(3);
            task.setErrorMsg(e.getMessage());
            taskMapper.updateById(task);

            Shot shot = shotMapper.selectById(shotId);
            if (shot != null) {
                shot.setPromptStatus("failed");
                shot.setErrorMsg(e.getMessage());
                shotMapper.updateById(shot);
            }
        }
    }

    @Override
    public Long generateImage(Long id) {
        Shot shot = getOwnedShot(id);
        if (shot.getPromptEn() == null || shot.getPromptEn().isBlank()) {
            throw new BusinessException(40900, "请先生成英文提示词");
        }
        List<String> refImages = getAssetReferenceImages(id, true);
        if (refImages.isEmpty()) {
            throw new BusinessException(40900, "请先绑定已确认且带主参考图的资产");
        }

        Task task = new Task();
        task.setType("image_gen");
        task.setShotId(id);
        Episode episode = episodeMapper.selectById(shot.getEpisodeId());
        if (episode != null) task.setProjectId(episode.getProjectId());
        task.setStatus(0);
        task.setPollCount(0);
        taskMapper.insert(task);

        // 同步提交，落库后再返回
        Long taskId = task.getId();

        executeImageGenAsync(taskId, id);

        return taskId;
    }

    @Async("aiTaskExecutor")
    public void executeImageGenAsync(Long taskId, Long shotId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;

        try {
            task.setStatus(1);
            taskMapper.updateById(task);

            Shot shot = shotMapper.selectById(shotId);
            shot.setImageStatus("generating");
            shotMapper.updateById(shot);
            List<String> refImages = getAssetReferenceImages(shotId, true);

            // 调用图片生成 API
            List<String> imageUrls = imageGenClient.generateImage(shot.getPromptEn(), refImages);

            if (imageUrls.isEmpty()) {
                task.setStatus(3);
                task.setErrorMsg("AI 生成图片为空");
                shot.setImageStatus("failed");
                shot.setErrorMsg("AI 生成图片为空");
            } else {
                // TODO: 下载图片上传到 TOS，回写 URL
                // 简化处理：先直接用返回的 URL
                String imageUrl = imageUrls.get(0);
                shot.setGeneratedImageUrl(imageUrl);
                shot.setImageStatus("success");
                task.setStatus(2);
                task.setResultUrl(imageUrl);
            }

            shotMapper.updateById(shot);
            taskMapper.updateById(task);

        } catch (Exception e) {
            log.error("图片生成失败, shotId: {}", shotId, e);
            task.setStatus(3);
            task.setErrorMsg("图片生成失败: " + e.getMessage());
            taskMapper.updateById(task);

            Shot shot = shotMapper.selectById(shotId);
            if (shot != null) {
                shot.setImageStatus("failed");
                shot.setErrorMsg("图片生成失败: " + e.getMessage());
                shotMapper.updateById(shot);
            }
        }
    }

    @Override
    public Long generateVideo(Long id) {
        Shot shot = getOwnedShot(id);
        if (shot.getPromptEn() == null || shot.getPromptEn().isBlank()) {
            throw new BusinessException(40900, "请先生成英文提示词");
        }
        List<String> refImages = getAssetReferenceImages(id, true);
        if (refImages.isEmpty()) {
            throw new BusinessException(40900, "请先绑定已确认且带主参考图的资产");
        }

        Task task = new Task();
        task.setType("video_gen");
        task.setShotId(id);
        Episode episode = episodeMapper.selectById(shot.getEpisodeId());
        if (episode != null) task.setProjectId(episode.getProjectId());
        task.setStatus(0);
        task.setPollCount(0);
        taskMapper.insert(task);

        Long taskId = task.getId();

        executeVideoGenAsync(taskId, id);

        return taskId;
    }

    @Async("aiTaskExecutor")
    public void executeVideoGenAsync(Long taskId, Long shotId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;

        try {
            task.setStatus(1);
            taskMapper.updateById(task);

            Shot shot = shotMapper.selectById(shotId);
            shot.setVideoStatus("generating");
            shotMapper.updateById(shot);
            List<String> refImages = getAssetReferenceImages(shotId, true);

            // 视频承接逻辑：如果 follow_last=1，取上一分镜尾帧
            String firstFrame = null;
            if (shot.getFollowLast() == 1) {
                LambdaQueryWrapper<Shot> prevWrapper = new LambdaQueryWrapper<>();
                prevWrapper.eq(Shot::getEpisodeId, shot.getEpisodeId())
                           .lt(Shot::getSortOrder, shot.getSortOrder())
                           .orderByDesc(Shot::getSortOrder)
                           .last("LIMIT 1");
                Shot prevShot = shotMapper.selectOne(prevWrapper);
                if (prevShot != null && prevShot.getLastFrameUrl() != null) {
                    firstFrame = prevShot.getLastFrameUrl();
                }
            }

            // 提交视频生成任务
            String providerTaskId = videoGenClient.submitVideoTask(shot.getPromptEn(), firstFrame, refImages);
            task.setProviderTaskId(providerTaskId);
            taskService.markTaskPolling(task);

            // TODO: 视频生成成功后需要轮询，在 TaskScheduler 中处理

        } catch (Exception e) {
            log.error("视频生成提交失败, shotId: {}", shotId, e);
            task.setStatus(3);
            task.setErrorMsg("视频生成失败: " + e.getMessage());
            taskMapper.updateById(task);

            Shot shot = shotMapper.selectById(shotId);
            if (shot != null) {
                shot.setVideoStatus("failed");
                shot.setErrorMsg("视频生成失败: " + e.getMessage());
                shotMapper.updateById(shot);
            }
        }
    }

    // ==================== 批量操作 ====================

    @Override
    public BatchResultVO batchPrompt(Long episodeId) {
        List<Shot> shots = getShotsByEpisodeId(episodeId);
        String batchId = UUID.randomUUID().toString();
        List<Long> taskIds = new ArrayList<>();

        for (Shot shot : shots) {
            if (shot.getPrompt() == null) continue;
            Task task = new Task();
            task.setType("prompt_gen");
            task.setShotId(shot.getId());
            task.setEpisodeId(episodeId);
            Episode ep = episodeMapper.selectById(episodeId);
            if (ep != null) task.setProjectId(ep.getProjectId());
            task.setBatchId(batchId);
            task.setStatus(0);
            task.setPollCount(0);
            taskMapper.insert(task);
            taskIds.add(task.getId());
            executePromptGenAsync(task.getId(), shot.getId());
        }

        BatchResultVO vo = new BatchResultVO();
        vo.setBatchId(batchId);
        vo.setTaskIds(taskIds);
        return vo;
    }

    @Override
    public BatchResultVO batchImage(Long episodeId) {
        List<Shot> shots = getShotsByEpisodeId(episodeId);
        String batchId = UUID.randomUUID().toString();
        List<Long> taskIds = new ArrayList<>();

        for (Shot shot : shots) {
            if (shot.getPromptEn() == null || shot.getPromptEn().isBlank()) continue;
            Task task = new Task();
            task.setType("image_gen");
            task.setShotId(shot.getId());
            task.setEpisodeId(episodeId);
            Episode ep = episodeMapper.selectById(episodeId);
            if (ep != null) task.setProjectId(ep.getProjectId());
            task.setBatchId(batchId);
            task.setStatus(0);
            task.setPollCount(0);
            taskMapper.insert(task);
            taskIds.add(task.getId());
            executeImageGenAsync(task.getId(), shot.getId());
        }

        BatchResultVO vo = new BatchResultVO();
        vo.setBatchId(batchId);
        vo.setTaskIds(taskIds);
        return vo;
    }

    @Override
    public BatchResultVO batchVideo(Long episodeId) {
        List<Shot> shots = getShotsByEpisodeId(episodeId);
        String batchId = UUID.randomUUID().toString();
        List<Long> taskIds = new ArrayList<>();

        for (Shot shot : shots) {
            if (shot.getPromptEn() == null || shot.getPromptEn().isBlank()) continue;
            Task task = new Task();
            task.setType("video_gen");
            task.setShotId(shot.getId());
            task.setEpisodeId(episodeId);
            Episode ep = episodeMapper.selectById(episodeId);
            if (ep != null) task.setProjectId(ep.getProjectId());
            task.setBatchId(batchId);
            task.setStatus(0);
            task.setPollCount(0);
            taskMapper.insert(task);
            taskIds.add(task.getId());
            executeVideoGenAsync(task.getId(), shot.getId());
        }

        BatchResultVO vo = new BatchResultVO();
        vo.setBatchId(batchId);
        vo.setTaskIds(taskIds);
        return vo;
    }

    // ==================== 内部方法 ====================

    /**
     * 安全地将对象序列化为 JSON 字符串
     */
    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON 序列化失败", e);
            return null;
        }
    }

    /**
     * 校验项目存在性和归属
     */
    private Project getProjectById(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }
        return project;
    }

    private String getPromptText(String promptKey) {
        LambdaQueryWrapper<PromptConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptConfig::getPromptKey, promptKey)
               .eq(PromptConfig::getDeleted, 0)
               .last("LIMIT 1");
        PromptConfig config = promptConfigMapper.selectOne(wrapper);
        if (config != null && config.getPromptText() != null) {
            return config.getPromptText();
        }
        throw new BusinessException(40400, "Prompt 配置不存在: " + promptKey);
    }

    private String buildSystemPrompt(String promptKey, String templateFileName) {
        String responseTemplate = getResponseTemplate(templateFileName);
        try {
            String promptText = getPromptText(promptKey);
            return promptText + "\n\n请严格按以下 JSON 模板返回，不要输出额外说明：\n" + responseTemplate;
        } catch (BusinessException e) {
            if (e.getCode() != 40400) {
                throw e;
            }
            return "请仅输出合法 JSON，且结构必须与以下模板完全一致：\n" + responseTemplate;
        }
    }

    private String getResponseTemplate(String templateFileName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/ai/" + templateFileName);
            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = FileCopyUtils.copyToByteArray(is);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("读取静态模板失败, file: {}", templateFileName, e);
            throw new BusinessException(40400, "AI 返回模板不存在: " + templateFileName);
        }
    }

    /**
     * 解析 AI 返回的 JSON 并创建分镜
     */
    private void parseAndCreateShots(Long episodeId, String aiResult) {
        try {
            JsonNode array = objectMapper.readTree(aiResult);
            int sortOrder = 0;
            if (array.isArray()) {
                for (JsonNode node : array) {
                    Shot shot = new Shot();
                    shot.setEpisodeId(episodeId);
                    shot.setPrompt(node.has("prompt") ? node.get("prompt").asText() : "");
                    shot.setDuration(node.has("duration") ? node.get("duration").asInt() : 10);
                    shot.setSceneType(node.has("sceneType") ? node.get("sceneType").asText() : null);
                    shot.setCameraMove(node.has("cameraMove") ? node.get("cameraMove").asText() : null);
                    shot.setSortOrder(sortOrder++);
                    shot.setFollowLast(1);
                    shot.setPromptStatus("pending");
                    shot.setImageStatus("pending");
                    shot.setVideoStatus("pending");
                    shotMapper.insert(shot);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析 AI 分镜结果失败", e);
        }
    }

    private void replaceEpisodesFromAiResult(Long projectId, String aiResult) {
        try {
            JsonNode root = objectMapper.readTree(aiResult);
            JsonNode episodesNode = root.has("episodes") ? root.get("episodes") : root;
            if (episodesNode == null || !episodesNode.isArray() || episodesNode.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_RESULT_INVALID.getCode(), "剧本分析结果为空");
            }

            LambdaQueryWrapper<Episode> existingWrapper = new LambdaQueryWrapper<>();
            existingWrapper.eq(Episode::getProjectId, projectId);
            List<Episode> existingEpisodes = episodeMapper.selectList(existingWrapper);
            if (!existingEpisodes.isEmpty()) {
                List<Long> episodeIds = existingEpisodes.stream().map(Episode::getId).toList();
                if (!episodeIds.isEmpty()) {
                    shotAssetRefMapper.delete(new LambdaQueryWrapper<ShotAssetRef>()
                            .inSql(ShotAssetRef::getShotId, "select id from shot where episode_id in (" +
                                    episodeIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")"));
                    shotMapper.delete(new LambdaQueryWrapper<Shot>().in(Shot::getEpisodeId, episodeIds));
                }
                episodeMapper.delete(existingWrapper);
            }

            int sortOrder = 0;
            for (JsonNode node : episodesNode) {
                Episode episode = new Episode();
                episode.setProjectId(projectId);
                episode.setTitle(node.hasNonNull("title") ? node.get("title").asText() : "第" + (sortOrder + 1) + "集");
                episode.setSummary(node.hasNonNull("summary") ? node.get("summary").asText() : null);
                episode.setContent(node.hasNonNull("content") ? node.get("content").asText() : "");
                episode.setSortOrder(sortOrder++);
                episode.setParseStatus("success");
                episodeMapper.insert(episode);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESULT_INVALID.getCode(), "剧本分析结果解析失败");
        }
    }

    /**
     * 批量绑定资产到分镜
     */
    private void bindAssetsToShot(Long shotId, List<Long> assetIds) {
        Long shotProjectId = getProjectIdByShotId(shotId);
        for (Long assetId : assetIds) {
            Asset asset = assetMapper.selectById(assetId);
            if (asset == null) continue;
            if (!Objects.equals(asset.getProjectId(), shotProjectId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            if (!"confirmed".equals(asset.getStatus())) {
                throw new BusinessException(40900, "仅已确认资产可被分镜绑定");
            }
            if (extractPrimaryImage(asset) == null) {
                throw new BusinessException(40900, "资产缺少主参考图，不能绑定到分镜");
            }

            LambdaQueryWrapper<ShotAssetRef> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ShotAssetRef::getShotId, shotId)
                   .eq(ShotAssetRef::getAssetId, assetId);
            if (shotAssetRefMapper.selectCount(wrapper) > 0) continue;

            ShotAssetRef ref = new ShotAssetRef();
            ref.setShotId(shotId);
            ref.setAssetId(assetId);
            ref.setAssetType(asset.getAssetType());
            shotAssetRefMapper.insert(ref);
        }
    }

    /**
     * 获取分集下所有分镜
     */
    private List<Shot> getShotsByEpisodeId(Long episodeId) {
        getOwnedEpisode(episodeId);
        LambdaQueryWrapper<Shot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shot::getEpisodeId, episodeId).orderByAsc(Shot::getSortOrder);
        return shotMapper.selectList(wrapper);
    }

    /**
     * Episode 转 VO
     */
    private EpisodeVO toEpisodeVO(Episode episode) {
        EpisodeVO vo = new EpisodeVO();
        vo.setId(episode.getId());
        vo.setProjectId(episode.getProjectId());
        vo.setTitle(episode.getTitle());
        vo.setSummary(episode.getSummary());
        vo.setSortOrder(episode.getSortOrder());
        vo.setContent(episode.getContent());
        vo.setParseStatus(episode.getParseStatus());
        vo.setParseError(episode.getParseError());
        vo.setCreateTime(episode.getCreateTime());
        vo.setUpdateTime(episode.getUpdateTime());
        return vo;
    }

    /**
     * Shot 转 VO
     */
    private ShotVO toShotVO(Shot shot,
                            java.util.Map<Long, List<ShotAssetRef>> refsByShot,
                            java.util.Map<Long, Asset> assetMap) {
        ShotVO vo = new ShotVO();
        vo.setId(shot.getId());
        vo.setEpisodeId(shot.getEpisodeId());
        vo.setSortOrder(shot.getSortOrder());
        vo.setPrompt(shot.getPrompt());
        vo.setPromptEn(shot.getPromptEn());
        vo.setDuration(shot.getDuration());
        vo.setSceneType(shot.getSceneType());
        vo.setCameraMove(shot.getCameraMove());
        vo.setGeneratedImageUrl(tosService.buildReadableUrl(shot.getGeneratedImageUrl()));
        vo.setGeneratedVideoUrl(tosService.buildReadableUrl(shot.getGeneratedVideoUrl()));
        vo.setLastFrameUrl(tosService.buildReadableUrl(shot.getLastFrameUrl()));
        vo.setFollowLast(shot.getFollowLast());
        vo.setDraftContent(shot.getDraftContent());
        vo.setPromptStatus(shot.getPromptStatus());
        vo.setImageStatus(shot.getImageStatus());
        vo.setVideoStatus(shot.getVideoStatus());
        vo.setErrorMsg(shot.getErrorMsg());
        vo.setCreateTime(shot.getCreateTime());
        vo.setUpdateTime(shot.getUpdateTime());

        // 填充关联资产
        List<ShotAssetRef> refs = refsByShot.get(shot.getId());
        if (refs != null && !refs.isEmpty()) {
            List<ShotAssetRefVO> assetRefVOs = new ArrayList<>();
            for (ShotAssetRef ref : refs) {
                ShotAssetRefVO refVO = new ShotAssetRefVO();
                refVO.setAssetId(ref.getAssetId());
                refVO.setAssetType(ref.getAssetType());
                Asset asset = assetMap.get(ref.getAssetId());
                if (asset != null) {
                    refVO.setAssetName(asset.getName());
                    String primaryImage = extractPrimaryImage(asset);
                    if (primaryImage != null) {
                        refVO.setPrimaryImage(tosService.buildReadableUrl(primaryImage));
                    }
                }
                assetRefVOs.add(refVO);
            }
            vo.setAssetRefs(assetRefVOs);
        }

        return vo;
    }

    private Episode getOwnedEpisode(Long episodeId) {
        Episode episode = episodeMapper.selectById(episodeId);
        if (episode == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        getProjectById(episode.getProjectId());
        return episode;
    }

    private Shot getOwnedShot(Long shotId) {
        Shot shot = shotMapper.selectById(shotId);
        if (shot == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        getOwnedEpisode(shot.getEpisodeId());
        return shot;
    }

    private Long getProjectIdByShotId(Long shotId) {
        Shot shot = getOwnedShot(shotId);
        Episode episode = episodeMapper.selectById(shot.getEpisodeId());
        return episode != null ? episode.getProjectId() : null;
    }

    private List<String> getAssetReferenceImages(Long shotId, boolean requireConfirmed) {
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getShotId, shotId);
        List<ShotAssetRef> refs = shotAssetRefMapper.selectList(refWrapper);

        if (refs.isEmpty()) return List.of();

        List<Long> assetIds = refs.stream().map(ShotAssetRef::getAssetId).distinct().toList();
        List<Asset> assets = assetMapper.selectBatchIds(assetIds);

        List<String> images = new ArrayList<>();
        for (Asset asset : assets) {
            if (requireConfirmed && !"confirmed".equals(asset.getStatus())) {
                continue;
            }
            String primaryImage = extractPrimaryImage(asset);
            if (primaryImage != null) {
                images.add(primaryImage);
            }
        }
        return images;
    }

    private String extractPrimaryImage(Asset asset) {
        if (asset.getReferenceImages() == null || asset.getReferenceImages().isBlank()) {
            return null;
        }
        try {
            JsonNode arr = objectMapper.readTree(asset.getReferenceImages());
            if (arr.isArray() && arr.size() > 0) {
                return arr.get(0).asText();
            }
        } catch (Exception e) {
            return asset.getReferenceImages();
        }
        return null;
    }
}
