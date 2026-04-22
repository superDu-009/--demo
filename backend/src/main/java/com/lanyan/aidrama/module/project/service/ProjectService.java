package com.lanyan.aidrama.module.project.service;

import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.module.project.dto.*;

import java.util.List;

/**
 * 项目服务接口 (系分 4.2.2)
 * 负责项目 CRUD、小说文件 TOS 路径绑定、流程配置保存
 */
public interface ProjectService {

    /**
     * 分页查用户项目列表，自动过滤 deleted=1
     */
    PageResult<ProjectVO> listProjects(Long userId, int page, int size);

    /**
     * 创建项目，绑定 user_id
     */
    Long createProject(ProjectCreateRequest req, Long userId);

    /**
     * 查详情，校验权限（项目归属）
     */
    ProjectVO getProjectDetail(Long id);

    /**
     * 更新项目字段
     */
    void updateProject(Long id, ProjectUpdateRequest req);

    /**
     * 删除项目，逻辑删除，需校验 execution_lock=0
     */
    void deleteProject(Long id);

    /**
     * 保存 workflow_config JSON，带乐观锁
     * 仅允许 status=草稿 时修改
     */
    void saveWorkflowConfig(Long id, WorkflowConfigRequest req);

    /**
     * 项目级分镜聚合查询，支持跨分场查看所有分镜（分页）
     */
    PageResult<ShotVO> listProjectShots(Long projectId, Long sceneId, Integer status, int page, int size);
}
