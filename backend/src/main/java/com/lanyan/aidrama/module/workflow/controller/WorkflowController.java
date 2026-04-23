package com.lanyan.aidrama.module.workflow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.workflow.dto.ReviewRequest;
import com.lanyan.aidrama.module.workflow.dto.WorkflowStatusVO;
import com.lanyan.aidrama.module.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 流程引擎 Controller (系分 4.5.1)
 * 负责流程启动、进度查询、审核、停止
 */
@Slf4j
@RestController
@RequestMapping("/api/project/{projectId}/workflow")
@RequiredArgsConstructor
@Tag(name = "流程引擎", description = "工作流启动、状态查询、停止")
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * 开始执行工作流
     */
    @PostMapping("/start")
    @Operation(summary = "开始执行工作流", description = "启动项目的自动生产流程")
    public Result<Void> startWorkflow(@PathVariable Long projectId) {
        workflowService.startWorkflow(projectId);
        return Result.ok(null);
    }

    /**
     * 查询工作流进度
     */
    @GetMapping("/status")
    @Operation(summary = "查询工作流进度", description = "获取流程执行状态和进度")
    public Result<WorkflowStatusVO> getWorkflowStatus(@PathVariable Long projectId) {
        WorkflowStatusVO status = workflowService.getWorkflowStatus(projectId);
        return Result.ok(status);
    }

    /**
     * 审核步骤
     */
    @PostMapping("/review")
    @Operation(summary = "审核步骤", description = "审核通过/打回某个步骤")
    public Result<Void> review(@PathVariable Long projectId,
                               @Valid @RequestBody ReviewRequest req) {
        workflowService.reviewStep(projectId, req.getStepType(), req.getAction(), req.getComment());
        return Result.ok(null);
    }

    /**
     * 停止工作流
     */
    @PostMapping("/stop")
    @Operation(summary = "停止工作流", description = "停止项目的自动生产流程")
    public Result<Void> stopWorkflow(@PathVariable Long projectId) {
        workflowService.stopWorkflow(projectId);
        return Result.ok(null);
    }
}
