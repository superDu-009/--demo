package com.lanyan.aidrama.module.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程步骤状态 VO (系分 4.5.1)
 */
@Data
@Schema(description = "流程步骤状态信息")
public class WorkflowStepStatusVO {

    @Schema(description = "步骤类型")
    private String stepType;

    @Schema(description = "步骤顺序")
    private int stepOrder;

    @Schema(description = "状态: 0-未执行 1-执行中 2-成功 3-失败 4-待审核")
    private int status;

    @Schema(description = "步骤内部进度百分比 0-100")
    private int progress;

    @Schema(description = "用户可读描述，如'正在生成第45/100个分镜'")
    private String currentDetail;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "审核意见")
    private String reviewComment;
}
