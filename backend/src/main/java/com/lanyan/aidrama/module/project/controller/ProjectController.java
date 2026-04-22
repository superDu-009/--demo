package com.lanyan.aidrama.module.project.controller;

import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.project.dto.*;
import com.lanyan.aidrama.module.project.service.ProjectService;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 项目模块 Controller (系分 4.2.1)
 * 提供项目 CRUD、流程配置保存、项目级分镜查询
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "项目模块", description = "项目 CRUD、工作流操作")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/list")
    @Operation(summary = "项目列表", description = "分页查询当前用户的项目列表")
    @Parameter(name = "page", description = "页码，默认1")
    @Parameter(name = "size", description = "每页大小，默认10")
    public Result<PageResult<ProjectVO>> listProjects(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<ProjectVO> result = projectService.listProjects(userId, page, size);
        return Result.ok(result);
    }

    @PostMapping
    @Operation(summary = "创建项目", description = "创建新的漫剧项目，返回项目ID")
    public Result<Long> createProject(@Valid @RequestBody ProjectCreateRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long projectId = projectService.createProject(req, userId);
        return Result.ok(projectId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询项目详情", description = "根据ID获取项目完整信息")
    @Parameter(name = "id", description = "项目ID", required = true)
    public Result<ProjectVO> getProject(@PathVariable Long id) {
        ProjectVO vo = projectService.getProjectDetail(id);
        return Result.ok(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目", description = "更新项目基本信息")
    @Parameter(name = "id", description = "项目ID", required = true)
    public Result<Void> updateProject(@PathVariable Long id,
                                       @Valid @RequestBody ProjectUpdateRequest req) {
        projectService.updateProject(id, req);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目", description = "逻辑删除项目，需项目未执行中")
    @Parameter(name = "id", description = "项目ID", required = true)
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.ok(null);
    }

    @PutMapping("/{id}/workflow")
    @Operation(summary = "保存流程配置", description = "保存线性流程配置和风格预设，仅草稿状态可修改")
    @Parameter(name = "id", description = "项目ID", required = true)
    public Result<Void> saveWorkflowConfig(@PathVariable Long id,
                                            @Valid @RequestBody WorkflowConfigRequest req) {
        projectService.saveWorkflowConfig(id, req);
        return Result.ok(null);
    }

    @GetMapping("/{projectId}/shots")
    @Operation(summary = "项目级分镜查询", description = "跨分场聚合查询分镜（分页），支持按分场和状态过滤")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    public Result<PageResult<ShotVO>> listProjectShots(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long sceneId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        PageResult<ShotVO> result = projectService.listProjectShots(projectId, sceneId, status, page, size);
        return Result.ok(result);
    }
}
