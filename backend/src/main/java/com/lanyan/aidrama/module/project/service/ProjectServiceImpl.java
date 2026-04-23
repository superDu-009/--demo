package com.lanyan.aidrama.module.project.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.common.ProjectStatus;
import com.lanyan.aidrama.entity.*;
import com.lanyan.aidrama.mapper.*;
import com.lanyan.aidrama.module.project.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目服务实现类 (系分 4.2.2)
 * 实现项目 CRUD、流程配置保存、项目级分镜聚合查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ShotMapper shotMapper;
    private final ShotAssetRefMapper shotAssetRefMapper;
    private final AssetMapper assetMapper;
    private final AiTaskMapper aiTaskMapper;
    private final EpisodeMapper episodeMapper;
    private final SceneMapper sceneMapper;

    @Override
    public PageResult<ProjectVO> listProjects(Long userId, int page, int size) {
        // 分页查询当前用户的项目，自动过滤 deleted=1
        Page<Project> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getUserId, userId)
               .orderByDesc(Project::getCreateTime);

        IPage<Project> pageResult = projectMapper.selectPage(pageParam, wrapper);

        // 转换为 VO
        List<ProjectVO> voList = pageResult.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return PageResult.of(pageResult.convert(this::toVO));
    }

    @Override
    public Long createProject(ProjectCreateRequest req, Long userId) {
        // 创建项目，绑定创建人ID
        Project project = new Project();
        project.setUserId(userId);
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setNovelTosPath(req.getNovelTosPath());
        project.setStatus(ProjectStatus.DRAFT);
        project.setExecutionLock(0);
        project.setVersion(0);

        projectMapper.insert(project);
        log.info("创建项目成功, projectId: {}, userId: {}", project.getId(), userId);
        return project.getId();
    }

    @Override
    public ProjectVO getProjectDetail(Long id) {
        Project project = getProjectById(id);
        return toVO(project);
    }

    @Override
    public void updateProject(Long id, ProjectUpdateRequest req) {
        Project project = getProjectById(id);

        if (req.getName() != null) {
            project.setName(req.getName());
        }
        if (req.getDescription() != null) {
            project.setDescription(req.getDescription());
        }
        if (req.getNovelTosPath() != null) {
            project.setNovelTosPath(req.getNovelTosPath());
        }
        if (req.getStatus() != null) {
            project.setStatus(req.getStatus());
        }

        projectMapper.updateById(project);
        log.info("更新项目成功, projectId: {}", id);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        Project project = getProjectById(id);

        if (project.getExecutionLock() == 1) {
            throw new BusinessException(ErrorCode.PROJECT_EXECUTING);
        }

        projectMapper.deleteById(id);
        log.info("删除项目成功, projectId: {}", id);
    }

    @Override
    public void saveWorkflowConfig(Long id, WorkflowConfigRequest req) {
        Project project = getProjectById(id);

        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BusinessException(ErrorCode.PROJECT_EXECUTING);
        }

        project.setWorkflowConfig(req.getWorkflowConfig());
        project.setStylePreset(req.getStylePreset());

        projectMapper.updateById(project);
        log.info("保存流程配置成功, projectId: {}", id);
    }

    @Override
    public PageResult<ShotVO> listProjectShots(Long projectId, Long sceneId, Integer status, int page, int size) {
        getProjectById(projectId);

        Page<Shot> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Shot> wrapper = new LambdaQueryWrapper<>();

        // 如果指定了 sceneId，按分场过滤；否则查询该项目下所有分场的分镜
        if (sceneId != null) {
            wrapper.eq(Shot::getSceneId, sceneId);
        } else {
            List<Long> sceneIds = getAllSceneIdsByProjectId(projectId);
            wrapper.in(Shot::getSceneId, sceneIds);
        }

        // 按状态过滤
        if (status != null) {
            wrapper.eq(Shot::getStatus, status);
        }

        wrapper.eq(Shot::getDeleted, 0)
               .orderByAsc(Shot::getSceneId)
               .orderByAsc(Shot::getSortOrder);

        IPage<Shot> pageResult = shotMapper.selectPage(pageParam, wrapper);

        // 批量查询关联数据，避免 N+1
        List<Shot> shots = pageResult.getRecords();
        if (shots.isEmpty()) {
            PageResult<ShotVO> result = new PageResult<>();
            result.setList(List.of());
            result.setTotal(pageResult.getTotal());
            result.setPage((int) pageResult.getCurrent());
            result.setSize((int) pageResult.getSize());
            result.setHasNext(false);
            return result;
        }

        List<Long> shotIds = shots.stream()
                .map(Shot::getId)
                .toList();

        // 批量查询资产关联
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.in(ShotAssetRef::getShotId, shotIds);
        List<ShotAssetRef> allRefs = shotAssetRefMapper.selectList(refWrapper);

        // 批量查询资产信息
        Map<Long, Asset> assetMap = Map.of();
        if (!allRefs.isEmpty()) {
            List<Long> assetIds = allRefs.stream()
                    .map(ShotAssetRef::getAssetId)
                    .distinct()
                    .toList();
            assetMap = assetMapper.selectBatchIds(assetIds).stream()
                    .collect(Collectors.toMap(Asset::getId, a -> a));
        }

        // 批量查询最新 AI 任务（每个 shotId 取最新一条）
        Map<Long, AiTask> taskMap = Map.of();
        if (!shotIds.isEmpty()) {
            LambdaQueryWrapper<AiTask> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.in(AiTask::getShotId, shotIds)
                       .orderByDesc(AiTask::getId);
            List<AiTask> allTasks = aiTaskMapper.selectList(taskWrapper);
            // 每个 shotId 只保留最新的任务
            taskMap = allTasks.stream()
                    .collect(Collectors.toMap(AiTask::getShotId, t -> t, (existing, replacement) -> existing));
        }

        final Map<Long, Asset> finalAssetMap = assetMap;
        final Map<Long, AiTask> finalTaskMap = taskMap;
        final List<ShotAssetRef> finalRefs = allRefs;

        // 按 shotId 分组资产关联
        Map<Long, List<ShotAssetRef>> refsByShot = finalRefs.stream()
                .collect(Collectors.groupingBy(ShotAssetRef::getShotId));

        // 转换为 VO
        List<ShotVO> voList = shots.stream()
                .map(shot -> toShotVO(shot, refsByShot, finalAssetMap, finalTaskMap))
                .toList();

        PageResult<ShotVO> result = new PageResult<>();
        result.setList(voList);
        result.setTotal(pageResult.getTotal());
        result.setPage((int) pageResult.getCurrent());
        result.setSize((int) pageResult.getSize());
        result.setHasNext(pageResult.getCurrent() * pageResult.getSize() < pageResult.getTotal());
        return result;
    }

    // ============ 内部方法 ============

    /**
     * 查询项目并校验存在性和归属
     */
    private Project getProjectById(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }

        return project;
    }

    /**
     * Project 转 VO
     */
    private ProjectVO toVO(Project project) {
        ProjectVO vo = new ProjectVO();
        vo.setId(project.getId());
        vo.setName(project.getName());
        vo.setDescription(project.getDescription());
        vo.setNovelTosPath(project.getNovelTosPath());
        vo.setWorkflowConfig(project.getWorkflowConfig());
        vo.setStylePreset(project.getStylePreset());
        vo.setStatus(project.getStatus());
        vo.setExecutionLock(project.getExecutionLock());
        vo.setVersion(project.getVersion());
        vo.setCreateTime(project.getCreateTime());
        vo.setUpdateTime(project.getUpdateTime());
        return vo;
    }

    /**
     * Shot 转 VO，使用预加载的关联数据避免 N+1 查询
     */
    private ShotVO toShotVO(Shot shot,
                            Map<Long, List<ShotAssetRef>> refsByShot,
                            Map<Long, Asset> assetMap,
                            Map<Long, AiTask> taskMap) {
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
                    if (asset.getReferenceImages() != null) {
                        refVO.setPrimaryImage(asset.getReferenceImages());
                    }
                }
                assetRefVOs.add(refVO);
            }
            vo.setAssetRefs(assetRefVOs);
        }

        // 填充最新 AI 任务
        AiTask latestTask = taskMap.get(shot.getId());
        if (latestTask != null) {
            ShotAiTaskVO taskVO = new ShotAiTaskVO();
            taskVO.setTaskId(latestTask.getId());
            taskVO.setTaskType(latestTask.getTaskType());
            taskVO.setStatus(latestTask.getStatus());
            vo.setCurrentAiTask(taskVO);
        }

        return vo;
    }

    /**
     * 获取项目下所有分场的 sceneId 列表
     */
    private List<Long> getAllSceneIdsByProjectId(Long projectId) {
        // episode -> scene 两级关联查询
        LambdaQueryWrapper<Episode> epWrapper = new LambdaQueryWrapper<>();
        epWrapper.eq(Episode::getProjectId, projectId)
                 .select(Episode::getId);
        List<Episode> episodes = episodeMapper.selectList(epWrapper);

        if (episodes.isEmpty()) {
            return List.of(-1L);
        }

        List<Long> episodeIds = episodes.stream()
                .map(Episode::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Scene> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.in(Scene::getEpisodeId, episodeIds)
                    .select(Scene::getId);
        return sceneMapper.selectList(sceneWrapper).stream()
                .map(Scene::getId)
                .collect(Collectors.toList());
    }
}
