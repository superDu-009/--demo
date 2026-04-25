package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 剧本分析状态视图
 */
@Data
@Schema(description = "剧本分析状态响应")
public class ScriptAnalyzeStatusVO {

    @Schema(description = "解析状态: pending/analyzing/success/failed")
    private String parseStatus;

    @Schema(description = "解析失败原因")
    private String parseError;

    @Schema(description = "当前分析任务ID")
    private Long taskId;
}
