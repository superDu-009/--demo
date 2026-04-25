package com.lanyan.aidrama.module.project.service;

import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.module.project.dto.*;

/**
 * 项目服务接口 (系分 v1.2 第 7.2 节)
 */
public interface ProjectService {

    /**
     * 分页查用户项目列表
     */
    PageResult<ProjectVO> listProjects(Long userId, int page, int size);

    /**
     * 创建项目
     */
    Long createProject(ProjectCreateRequest req, Long userId);

    /**
     * 查详情
     */
    ProjectVO getProjectDetail(Long id);

    /**
     * 更新项目基本信息
     */
    void updateProject(Long id, ProjectUpdateRequest req);

    /**
     * 删除项目（逻辑删除）
     */
    void deleteProject(Long id);
}
