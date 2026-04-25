package com.lanyan.aidrama.module.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建资产请求 DTO (系分 v1.2 第 7.4 节)
 */
@Data
@Schema(description = "创建资产请求参数")
public class AssetCreateRequest {

    @Schema(description = "资产类型: character/scene/prop/voice", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "资产名称不能为空")
    private String name;

    @Schema(description = "AI描述文本")
    private String description;

    @Schema(description = "参考图URL数组JSON字符串")
    private String referenceImages;

    @Schema(description = "父资产ID数组(JSON)")
    private String parentIds;
}
