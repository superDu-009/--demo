package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量审核请求 DTO (系分 4.4.1)
 */
@Data
@Schema(description = "批量审核请求参数")
public class BatchReviewRequest {

    @Schema(description = "分镜ID数组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "分镜ID列表不能为空")
    private List<Long> shotIds;

    @Schema(description = "操作: approve(通过) / reject(打回)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作类型不能为空")
    private String action;

    @Schema(description = "审核意见，打回时必填")
    private String comment;
}
