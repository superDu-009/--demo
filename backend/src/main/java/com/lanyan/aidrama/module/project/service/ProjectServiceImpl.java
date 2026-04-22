package com.lanyan.aidrama.module.project.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.entity.*;
import com.lanyan.aidrama.mapper.*;
import com.lanyan.aidrama.module.project.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
        project.setStatus(0); // 默认草稿
        project.setExecutionLock(0);
        project.setVersion(0);

        projectMapper.insert(project);
        log.info("创建项目成功, projectId: {}, userId: {}", project.getId(), userId);
        return project.getId();
    }

    @Override
    public ProjectVO getProjectDetail(Long id) {
        // 查询项目详情，校验权限（项目归属）
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 校验项目归属
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }

        return toVO(project);
    }

    @Override
    public void updateProject(Long id, ProjectUpdateRequest req) {
        // 查询项目
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 校验项目归属
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }

        // 更新字段（非空字段才更新）
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
        // 查询项目
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 校验项目归属
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }

        // 校验 execution_lock，正在执行中不允许删除
        if (project.getExecutionLock() == 1) {
            throw new BusinessException(ErrorCode.PROJECT_EXECUTING);
        }

        // 逻辑删除
        projectMapper.deleteById(id);
        log.info("删除项目成功, projectId: {}", id);
    }

    @Override
    public void saveWorkflowConfig(Long id, WorkflowConfigRequest req) {
        // 查询项目
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 校验项目归属
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }

        // 仅允许草稿状态修改流程配置
        if (project.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PROJECT_EXECUTING);
        }

        // 更新流程配置和风格预设
        project.setWorkflowConfig(req.getWorkflowConfig());
        project.setStylePreset(req.getStylePreset());

        projectMapper.updateById(project);
        log.info("保存流程配置成功, projectId: {}", id);
    }

    @Override
    public PageResult<ShotVO> listProjectShots(Long projectId, Long sceneId, Integer status, int page, int size) {
        // 校验项目归属
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }

        // 构建查询条件：跨分场查询，通过 scene 关联到 episode，再关联到 project
        Page<Shot> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Shot> wrapper = new LambdaQueryWrapper<>();

        // 如果指定了 sceneId，按分场过滤；否则查询该项目下所有分场的分镜
        if (sceneId != null) {
            wrapper.eq(Shot::getSceneId, sceneId);
        } else {
            // 查询该项目下所有分场的分镜（需要通过子查询或 in 查询）
            // 简化方案：先查所有 scene，然后查 shot
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

        // 转换为 VO，填充 assetRefs 和 currentAiTask
        List<ShotVO> voList = pageResult.getRecords().stream()
                .map(shot -> toShotVO(shot, projectId))
                .collect(Collectors.toList());

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
     * Shot 转 VO，填充关联资产和当前AI任务
     */
    private ShotVO toShotVO(Shot shot, Long projectId) {
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

                // 查询资产名称和主图
                Asset asset = assetMapper.selectById(ref.getAssetId());
                if (asset != null) {
                    refVO.setAssetName(asset.getName());
                    // 主图是 reference_images JSON 数组的第一个元素
                    if (asset.getReferenceImages() != null) {
                        // 简化处理：直接使用 JSON 字符串
                        refVO.setPrimaryImage(asset.getReferenceImages());
                    }
                }
                assetRefVOs.add(refVO);
            }
            vo.setAssetRefs(assetRefVOs);
        }

        // 查询当前分镜最新的 AI 任务
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
            return List.of(-1L); // 返回不存在的ID，使 in 查询返回空
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
