package com.lanyan.aidrama.module.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务视图对象 (系分 v1.2 第 7.5 节)
 */
@Data
@Schema(description = "任务信息视图")
public class TaskVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务类型: script_analyze/shot_split/asset_extract/prompt_gen/image_gen/video_gen")
    private String type;

    @Schema(description = "状态: 0-待处理 1-处理中 2-成功 3-失败")
    private Integer status;

    @Schema(description = "进度百分比 0-100")
    private Integer progress;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "结果数据(JSON)")
    private String resultData;

    @Schema(description = "结果URL")
    private String resultUrl;

    @Schema(description = "批量ID")
    private String batchId;
}
