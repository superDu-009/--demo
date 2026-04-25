package com.lanyan.aidrama.module.task.controller;

import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.task.dto.BatchTaskStatusRequest;
import com.lanyan.aidrama.module.task.dto.TaskVO;
import com.lanyan.aidrama.module.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统一任务模块 Controller (系分 v1.2 第 7.5 节)
 */
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
@Tag(name = "统一任务", description = "任务状态查询")
public class TaskController {

    private final TaskService taskService;

    /**
     * 查询任务状态 (GET /api/task/{id})
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询任务状态", description = "返回 id/type/status/progress/errorMsg/resultData/resultUrl/batchId")
    public Result<TaskVO> getTaskStatus(@PathVariable Long id) {
        return Result.ok(taskService.getTaskStatus(id));
    }

    /**
     * 批量任务状态查询 (POST /api/task/batch/status)
     * 支持按 taskIds 或 batchId 查询
     */
    @PostMapping("/batch/status")
    @Operation(summary = "批量任务状态查询", description = "支持按 taskIds 或 batchId 查询")
    public Result<List<TaskVO>> batchStatus(@Valid @RequestBody BatchTaskStatusRequest req) {
        return Result.ok(taskService.batchStatus(req));
    }
}
