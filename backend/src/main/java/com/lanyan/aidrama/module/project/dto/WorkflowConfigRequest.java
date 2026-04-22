package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存流程配置请求 DTO (系分 4.2.1)
 */
@Data
@Schema(description = "保存流程配置请求参数")
public class WorkflowConfigRequest {

    @Schema(description = "当前乐观锁版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "版本号不能为空")
    private Integer version;

    @Schema(description = "流程配置JSON")
    private String workflowConfig;

    @Schema(description = "风格预设JSON")
    private String stylePreset;
}
