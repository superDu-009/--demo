package com.lanyan.aidrama.module.content.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.entity.*;
import com.lanyan.aidrama.mapper.*;
import com.lanyan.aidrama.module.content.dto.*;
import com.lanyan.aidrama.module.project.dto.ShotAiTaskVO;
import com.lanyan.aidrama.module.project.dto.ShotAssetRefVO;
import com.lanyan.aidrama.module.project.dto.ShotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 内容服务实现类 (系分 4.4.2)
 * 实现分集/分场/分镜 CRUD、批量审核、资产绑定
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final EpisodeMapper episodeMapper;
    private final SceneMapper sceneMapper;
    private final ShotMapper shotMapper;
    private final ShotAssetRefMapper shotAssetRefMapper;
    private final AssetMapper assetMapper;
    private final AiTaskMapper aiTaskMapper;

    // ===== 分集相关 =====

    @Override
    public List<EpisodeVO> listEpisodes(Long projectId) {
        LambdaQueryWrapper<Episode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Episode::getProjectId, projectId)
               .orderByAsc(Episode::getSortOrder);

        return episodeMapper.selectList(wrapper).stream()
                .map(this::toEpisodeVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long createEpisode(Long projectId, EpisodeCreateRequest req) {
        // 获取当前最大 sort_order，自动递增
        LambdaQueryWrapper<Episode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Episode::getProjectId, projectId)
               .orderByDesc(Episode::getSortOrder)
               .last("LIMIT 1");
        Episode maxOrderEpisode = episodeMapper.selectOne(wrapper);
        int sortOrder = (maxOrderEpisode != null) ? maxOrderEpisode.getSortOrder() + 1 : 0;

        Episode episode = new Episode();
        episode.setProjectId(projectId);
        episode.setTitle(req.getTitle());
        episode.setContent(req.getContent());
        episode.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : sortOrder);
        episode.setStatus(0); // 默认待处理

        episodeMapper.insert(episode);
        log.info("创建分集成功, episodeId: {}, projectId: {}", episode.getId(), projectId);
        return episode.getId();
    }

    @Override
    public void updateEpisode(Long id, EpisodeUpdateRequest req) {
        Episode episode = episodeMapper.selectById(id);
        if (episode == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (req.getTitle() != null) {
            episode.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            episode.setContent(req.getContent());
        }
        if (req.getSortOrder() != null) {
            episode.setSortOrder(req.getSortOrder());
        }
        if (req.getStatus() != null) {
            episode.setStatus(req.getStatus());
        }

        episodeMapper.updateById(episode);
        log.info("更新分集成功, episodeId: {}", id);
    }

    @Override
    @Transactional
    public void deleteEpisode(Long id) {
        Episode episode = episodeMapper.selectById(id);
        if (episode == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 级联逻辑删除：分场 → 分镜 → 资产关联
        // 1. 查询该分集下所有分场
        LambdaQueryWrapper<Scene> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(Scene::getEpisodeId, id).select(Scene::getId);
        List<Long> sceneIds = sceneMapper.selectList(sceneWrapper).stream()
                .map(Scene::getId).collect(Collectors.toList());

        if (!sceneIds.isEmpty()) {
            // 2. 查询分镜并删除关联资产
            LambdaQueryWrapper<Shot> shotWrapper = new LambdaQueryWrapper<>();
            shotWrapper.in(Shot::getSceneId, sceneIds).select(Shot::getId);
            List<Long> shotIds = shotMapper.selectList(shotWrapper).stream()
                    .map(Shot::getId).collect(Collectors.toList());

            if (!shotIds.isEmpty()) {
                // 3. 删除分镜-资产关联
                LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
                refWrapper.in(ShotAssetRef::getShotId, shotIds);
                shotAssetRefMapper.delete(refWrapper);

                // 4. 删除分镜
                shotMapper.delete(new LambdaQueryWrapper<Shot>().in(Shot::getId, shotIds));
            }

            // 5. 删除分场
            sceneMapper.delete(new LambdaQueryWrapper<Scene>().in(Scene::getId, sceneIds));
        }

        // 6. 删除分集
        episodeMapper.deleteById(id);
        log.info("级联删除分集成功, episodeId: {}", id);
    }

    // ===== 分场相关 =====

    @Override
    public List<SceneVO> listScenes(Long episodeId) {
        LambdaQueryWrapper<Scene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Scene::getEpisodeId, episodeId)
               .orderByAsc(Scene::getSortOrder);

        return sceneMapper.selectList(wrapper).stream()
                .map(this::toSceneVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long createScene(Long episodeId, SceneCreateRequest req) {
        // 获取当前最大 sort_order
        LambdaQueryWrapper<Scene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Scene::getEpisodeId, episodeId)
               .orderByDesc(Scene::getSortOrder)
               .last("LIMIT 1");
        Scene maxOrderScene = sceneMapper.selectOne(wrapper);
        int sortOrder = (maxOrderScene != null) ? maxOrderScene.getSortOrder() + 1 : 0;

        Scene scene = new Scene();
        scene.setEpisodeId(episodeId);
        scene.setTitle(req.getTitle());
        scene.setContent(req.getContent());
        scene.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : sortOrder);
        scene.setStatus(0);

        sceneMapper.insert(scene);
        log.info("创建分场成功, sceneId: {}, episodeId: {}", scene.getId(), episodeId);
        return scene.getId();
    }

    @Override
    public void updateScene(Long id, SceneUpdateRequest req) {
        Scene scene = sceneMapper.selectById(id);
        if (scene == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (req.getTitle() != null) {
            scene.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            scene.setContent(req.getContent());
        }
        if (req.getSortOrder() != null) {
            scene.setSortOrder(req.getSortOrder());
        }
        if (req.getStatus() != null) {
            scene.setStatus(req.getStatus());
        }

        sceneMapper.updateById(scene);
        log.info("更新分场成功, sceneId: {}", id);
    }

    @Override
    @Transactional
    public void deleteScene(Long id) {
        Scene scene = sceneMapper.selectById(id);
        if (scene == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 级联删除：分镜 → 资产关联
        LambdaQueryWrapper<Shot> shotWrapper = new LambdaQueryWrapper<>();
        shotWrapper.eq(Shot::getSceneId, id).select(Shot::getId);
        List<Long> shotIds = shotMapper.selectList(shotWrapper).stream()
                .map(Shot::getId).collect(Collectors.toList());

        if (!shotIds.isEmpty()) {
            LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
            refWrapper.in(ShotAssetRef::getShotId, shotIds);
            shotAssetRefMapper.delete(refWrapper);

            shotMapper.delete(new LambdaQueryWrapper<Shot>().in(Shot::getId, shotIds));
        }

        sceneMapper.deleteById(id);
        log.info("级联删除分场成功, sceneId: {}", id);
    }

    // ===== 分镜相关 =====

    @Override
    public PageResult<ShotVO> listShots(Long sceneId, int page, int size, Integer status) {
        Page<Shot> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Shot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shot::getSceneId, sceneId)
               .eq(status != null, Shot::getStatus, status)
               .eq(Shot::getDeleted, 0)
               .orderByAsc(Shot::getSortOrder);

        IPage<Shot> pageResult = shotMapper.selectPage(pageParam, wrapper);

        List<ShotVO> voList = pageResult.getRecords().stream()
                .map(this::toShotVO)
                .collect(Collectors.toList());

        PageResult<ShotVO> result = new PageResult<>();
        result.setList(voList);
        result.setTotal(pageResult.getTotal());
        result.setPage((int) pageResult.getCurrent());
        result.setSize((int) pageResult.getSize());
        result.setHasNext(pageResult.getCurrent() * pageResult.getSize() < pageResult.getTotal());
        return result;
    }

    @Override
    public Long createShot(Long sceneId, ShotCreateRequest req) {
        // 获取当前最大 sort_order
        LambdaQueryWrapper<Shot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shot::getSceneId, sceneId)
               .orderByDesc(Shot::getSortOrder)
               .last("LIMIT 1");
        Shot maxOrderShot = shotMapper.selectOne(wrapper);
        int sortOrder = (maxOrderShot != null) ? maxOrderShot.getSortOrder() + 1 : 0;

        Shot shot = new Shot();
        shot.setSceneId(sceneId);
        shot.setPrompt(req.getPrompt());
        shot.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : sortOrder);
        shot.setStatus(0); // 默认待处理
        shot.setVersion(1);
        shot.setGenerationAttempts(0);

        shotMapper.insert(shot);
        log.info("创建分镜成功, shotId: {}, sceneId: {}", shot.getId(), sceneId);
        return shot.getId();
    }

    @Override
    public void updateShot(Long id, ShotUpdateRequest req) {
        Shot shot = shotMapper.selectById(id);
        if (shot == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (req.getPrompt() != null) {
            shot.setPrompt(req.getPrompt());
        }
        if (req.getPromptEn() != null) {
            shot.setPromptEn(req.getPromptEn());
        }
        if (req.getSortOrder() != null) {
            shot.setSortOrder(req.getSortOrder());
        }
        if (req.getGeneratedImageUrl() != null) {
            shot.setGeneratedImageUrl(req.getGeneratedImageUrl());
        }
        if (req.getGeneratedVideoUrl() != null) {
            shot.setGeneratedVideoUrl(req.getGeneratedVideoUrl());
        }

        shotMapper.updateById(shot);
        log.info("更新分镜成功, shotId: {}", id);
    }

    @Override
    public void deleteShot(Long id) {
        Shot shot = shotMapper.selectById(id);
        if (shot == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 删除资产关联
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getShotId, id);
        shotAssetRefMapper.delete(refWrapper);

        shotMapper.deleteById(id);
        log.info("删除分镜成功, shotId: {}", id);
    }

    @Override
    @Transactional
    public BatchReviewResult batchReviewShots(List<Long> shotIds, String action, String comment) {
        BatchReviewResult result = new BatchReviewResult();
        result.setTotalCount(shotIds.size());
        result.setSuccessCount(0);
        result.setFailedCount(0);
        result.setFailedDetails(new ArrayList<>());

        for (Long shotId : shotIds) {
            Shot shot = shotMapper.selectById(shotId);
            if (shot == null) {
                addFailure(result, shotId, "分镜不存在");
                continue;
            }

            // 校验：只有生成中或待审核状态才能审核
            if (shot.getStatus() != 1 && shot.getStatus() != 2) {
                addFailure(result, shotId, "分镜状态不支持当前操作");
                continue;
            }

            if ("approve".equals(action)) {
                // 通过
                shot.setStatus(5); // 已完成
                shotMapper.updateById(shot);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } else if ("reject".equals(action)) {
                // 打回
                if (comment == null || comment.isBlank()) {
                    addFailure(result, shotId, "打回时必须填写审核意见");
                    continue;
                }
                shot.setStatus(4); // 已打回
                shot.setReviewComment(comment);
                shotMapper.updateById(shot);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } else {
                addFailure(result, shotId, "不支持的操作类型");
            }
        }

        result.setFailedCount(result.getTotalCount() - result.getSuccessCount());
        log.info("批量审核完成, 总数: {}, 成功: {}, 失败: {}", result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
        return result;
    }

    @Override
    @Transactional
    public void bindAssetToShot(Long shotId, Long assetId, String assetType) {
        Shot shot = shotMapper.selectById(shotId);
        if (shot == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Asset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 防止重复绑定（利用唯一约束）
        LambdaQueryWrapper<ShotAssetRef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShotAssetRef::getShotId, shotId)
               .eq(ShotAssetRef::getAssetId, assetId);
        Long count = shotAssetRefMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.SHOT_STATUS_NOT_SUPPORT);
        }

        ShotAssetRef ref = new ShotAssetRef();
        ref.setShotId(shotId);
        ref.setAssetId(assetId);
        ref.setAssetType(assetType);
        shotAssetRefMapper.insert(ref);
        log.info("绑定资产到分镜成功, shotId: {}, assetId: {}", shotId, assetId);
    }

    @Override
    @Transactional
    public void unbindAssetFromShot(Long shotId, Long assetId) {
        LambdaQueryWrapper<ShotAssetRef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShotAssetRef::getShotId, shotId)
               .eq(ShotAssetRef::getAssetId, assetId);
        shotAssetRefMapper.delete(wrapper);
        log.info("解绑分镜资产成功, shotId: {}, assetId: {}", shotId, assetId);
    }

    // ===== 内部方法 =====

    private EpisodeVO toEpisodeVO(Episode episode) {
        EpisodeVO vo = new EpisodeVO();
        vo.setId(episode.getId());
        vo.setProjectId(episode.getProjectId());
        vo.setTitle(episode.getTitle());
        vo.setSortOrder(episode.getSortOrder());
        vo.setContent(episode.getContent());
        vo.setStatus(episode.getStatus());
        vo.setCreateTime(episode.getCreateTime());
        vo.setUpdateTime(episode.getUpdateTime());
        return vo;
    }

    private SceneVO toSceneVO(Scene scene) {
        SceneVO vo = new SceneVO();
        vo.setId(scene.getId());
        vo.setEpisodeId(scene.getEpisodeId());
        vo.setTitle(scene.getTitle());
        vo.setSortOrder(scene.getSortOrder());
        vo.setContent(scene.getContent());
        vo.setStatus(scene.getStatus());
        vo.setCreateTime(scene.getCreateTime());
        vo.setUpdateTime(scene.getUpdateTime());
        return vo;
    }

    private ShotVO toShotVO(Shot shot) {
        ShotVO vo = new ShotVO();
        vo.setId(shot.getId());
        vo.setSceneId(shot.getSceneId());
        vo.setSortOrder(shot.getSortOrder());
        vo.setPrompt(shot.getPrompt());
        vo.setPromptEn(shot.getPromptEn());
        vo.setGeneratedImageUrl(shot.getGeneratedImageUrl());
        vo.setGeneratedVideoUrl(shot.getGeneratedVideoUrl());
        vo.setStatus(shot.getStatus());
        vo.setReviewComment(shot.getReviewComment());
        vo.setVersion(shot.getVersion());
        vo.setGenerationAttempts(shot.getGenerationAttempts());

        // 查询关联资产
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getShotId, shot.getId());
        List<ShotAssetRef> refs = shotAssetRefMapper.selectList(refWrapper);

        if (!refs.isEmpty()) {
            List<ShotAssetRefVO> assetRefVOs = new ArrayList<>();
            for (ShotAssetRef ref : refs) {
                ShotAssetRefVO refVO = new ShotAssetRefVO();
                refVO.setAssetId(ref.getAssetId());
                refVO.setAssetType(ref.getAssetType());
                Asset asset = assetMapper.selectById(ref.getAssetId());
                if (asset != null) {
                    refVO.setAssetName(asset.getName());
                    refVO.setPrimaryImage(asset.getReferenceImages());
                }
                assetRefVOs.add(refVO);
            }
            vo.setAssetRefs(assetRefVOs);
        }

        // 查询最新AI任务
        LambdaQueryWrapper<AiTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(AiTask::getShotId, shot.getId())
                   .orderByDesc(AiTask::getId)
                   .last("LIMIT 1");
        AiTask latestTask = aiTaskMapper.selectOne(taskWrapper);
        if (latestTask != null) {
            ShotAiTaskVO taskVO = new ShotAiTaskVO();
            taskVO.setTaskId(latestTask.getId());
            taskVO.setTaskType(latestTask.getTaskType());
            taskVO.setStatus(latestTask.getStatus());
            vo.setCurrentAiTask(taskVO);
        }

        return vo;
    }

    private void addFailure(BatchReviewResult result, Long shotId, String reason) {
        BatchReviewResult.FailedDetail detail = new BatchReviewResult.FailedDetail();
        detail.setShotId(shotId);
        detail.setReason(reason);
        result.getFailedDetails().add(detail);
    }
}
