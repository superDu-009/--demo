package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分场创建请求 DTO
 */
@Data
@Schema(description = "创建分场请求参数")
public class SceneCreateRequest {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "分场描述")
    private String content;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
