package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分镜创建请求 DTO
 */
@Data
@Schema(description = "创建分镜请求参数")
public class ShotCreateRequest {

    @Schema(description = "AI生图提示词(中文)")
    private String prompt;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
