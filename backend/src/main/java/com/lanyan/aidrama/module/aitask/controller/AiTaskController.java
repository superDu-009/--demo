package com.lanyan.aidrama.module.aitask.controller;

import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.aitask.dto.AiTaskVO;
import com.lanyan.aidrama.module.aitask.service.AiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI 任务 Controller (系分 4.6.1)
 * 负责 AI 任务状态查询
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/task")
@RequiredArgsConstructor
@Tag(name = "AI任务", description = "AI 任务提交、状态查询")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    /**
     * 查询 AI 任务状态
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "查询AI任务状态", description = "根据任务ID查询AI任务的执行状态")
    @Parameter(name = "taskId", description = "AI任务ID", required = true)
    public Result<AiTaskVO> getTaskStatus(@PathVariable Long taskId) {
        AiTaskVO task = aiTaskService.getTaskStatus(taskId);
        return Result.ok(task);
    }

    /**
     * 查询分镜最新 AI 任务
     */
    @GetMapping("/latest")
    @Operation(summary = "查询分镜最新AI任务", description = "按 shotId 查询分镜下最新的 AI 任务状态")
    @Parameter(name = "shotId", description = "分镜ID", required = true)
    public Result<AiTaskVO> getLatestTask(@RequestParam Long shotId) {
        AiTaskVO task = aiTaskService.getLatestTaskByShotId(shotId);
        return Result.ok(task);
    }
}
