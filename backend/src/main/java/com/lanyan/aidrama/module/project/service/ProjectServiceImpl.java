package com.lanyan.aidrama.module.project.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.entity.Project;
import com.lanyan.aidrama.mapper.ProjectMapper;
import com.lanyan.aidrama.module.project.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 项目服务实现类 (系分 v1.2 第 7.2 节)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;

    @Override
    public PageResult<ProjectVO> listProjects(Long userId, int page, int size) {
        Page<Project> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getUserId, userId)
               .orderByDesc(Project::getCreateTime);

        IPage<Project> pageResult = projectMapper.selectPage(pageParam, wrapper);

        PageResult<ProjectVO> result = new PageResult<>();
        result.setTotal(pageResult.getTotal());
        result.setPage((int) pageResult.getCurrent());
        result.setSize((int) pageResult.getSize());
        result.setHasNext(pageResult.getCurrent() * pageResult.getSize() < pageResult.getTotal());
        result.setList(pageResult.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public Long createProject(ProjectCreateRequest req, Long userId) {
        Project project = new Project();
        project.setUserId(userId);
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setRatio(req.getRatio() != null ? req.getRatio() : "16:9");
        project.setDefinition(req.getDefinition() != null ? req.getDefinition() : "1080P");
        project.setStyle(req.getStyle());
        project.setStyleDesc(req.getStyleDesc());

        // 小说文件信息（前端预签名上传后传入 fileKey）
        if (req.getNovelFile() != null && req.getNovelFile().getFileKey() != null) {
            project.setNovelOriginalTosPath(req.getNovelFile().getFileKey());
        }

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

        projectMapper.updateById(project);
        log.info("更新项目成功, projectId: {}", id);
    }

    @Override
    public void deleteProject(Long id) {
        Project project = getProjectById(id);
        projectMapper.deleteById(id);
        log.info("删除项目成功, projectId: {}", id);
    }

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
        vo.setNovelOriginalTosPath(project.getNovelOriginalTosPath());
        vo.setNovelTosPath(project.getNovelTosPath());
        vo.setRatio(project.getRatio());
        vo.setDefinition(project.getDefinition());
        vo.setStyle(project.getStyle());
        vo.setStyleDesc(project.getStyleDesc());
        vo.setCreateTime(project.getCreateTime());
        vo.setUpdateTime(project.getUpdateTime());
        return vo;
    }
}
