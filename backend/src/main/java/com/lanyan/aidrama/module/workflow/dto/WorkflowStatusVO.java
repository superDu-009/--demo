package com.lanyan.aidrama.module.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流程状态响应 VO (系分 4.5.1)
 */
@Data
@Schema(description = "流程执行状态信息")
public class WorkflowStatusVO {

    @Schema(description = "执行锁: 0-未执行 1-执行中")
    private int executionLock;

    @Schema(description = "当前执行的 stepType")
    private String currentStep;

    @Schema(description = "当前处理的分集ID")
    private Long currentEpisodeId;

    @Schema(description = "当前分集标题")
    private String currentEpisodeTitle;

    @Schema(description = "总分集数")
    private int totalEpisodes;

    @Schema(description = "整体进度百分比 0-100")
    private int overallProgress;

    @Schema(description = "总待处理分镜数")
    private int totalShots;

    @Schema(description = "已处理分镜数")
    private int processedShots;

    @Schema(description = "预估剩余秒数")
    private int estimatedRemainingSeconds;

    @Schema(description = "各步骤状态详情")
    private List<WorkflowStepStatusVO> steps;
}
