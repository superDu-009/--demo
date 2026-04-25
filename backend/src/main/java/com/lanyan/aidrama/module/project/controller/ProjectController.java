package com.lanyan.aidrama.module.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.project.dto.*;
import com.lanyan.aidrama.module.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 项目模块 Controller (系分 v1.2 第 7.2 节)
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "项目模块", description = "项目 CRUD")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 项目列表 (GET /api/project/list)
     */
    @GetMapping("/list")
    @Operation(summary = "项目列表", description = "分页查询当前用户的项目列表")
    public Result<PageResult<ProjectVO>> listProjects(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(projectService.listProjects(userId, page, size));
    }

    /**
     * 创建项目 (POST /api/project)
     */
    @PostMapping
    @Operation(summary = "创建项目", description = "创建新的漫剧项目")
    public Result<Long> createProject(@Valid @RequestBody ProjectCreateRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(projectService.createProject(req, userId));
    }

    /**
     * 项目详情 (GET /api/project/{id})
     */
    @GetMapping("/{id}")
    @Operation(summary = "项目详情", description = "根据ID获取项目完整信息")
    public Result<ProjectVO> getProject(@PathVariable Long id) {
        return Result.ok(projectService.getProjectDetail(id));
    }

    /**
     * 更新项目 (PUT /api/project/{id})
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新项目", description = "更新项目基本信息")
    public Result<Void> updateProject(@PathVariable Long id,
                                       @Valid @RequestBody ProjectUpdateRequest req) {
        projectService.updateProject(id, req);
        return Result.ok(null);
    }

    /**
     * 删除项目 (DELETE /api/project/{id})
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目", description = "逻辑删除项目")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.ok(null);
    }
}
