package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分镜最新AI任务 DTO
 */
@Data
@Schema(description = "分镜最新AI任务")
public class ShotAiTaskVO {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务类型: image_gen/video_gen")
    private String taskType;

    @Schema(description = "状态: 0-提交中 1-处理中 2-成功 3-失败")
    private Integer status;
}
