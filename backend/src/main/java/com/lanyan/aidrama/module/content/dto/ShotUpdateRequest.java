package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分镜更新请求 DTO
 */
@Data
@Schema(description = "更新分镜请求参数")
public class ShotUpdateRequest {

    @Schema(description = "AI生图提示词(中文)")
    private String prompt;

    @Schema(description = "AI生图提示词(英文)")
    private String promptEn;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "生成图片URL")
    private String generatedImageUrl;

    @Schema(description = "生成视频URL")
    private String generatedVideoUrl;
}
