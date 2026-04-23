package com.lanyan.aidrama.module.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程审核请求 (系分 4.5.1)
 */
@Data
@Schema(description = "流程审核请求")
public class ReviewRequest {

    @Schema(description = "审核步骤类型")
    @NotBlank(message = "步骤类型不能为空")
    private String stepType;

    @Schema(description = "审核操作: approve(通过) / reject(打回)")
    @NotBlank(message = "审核操作不能为空")
    private String action;

    @Schema(description = "审核意见，打回时必填")
    private String comment;
}
