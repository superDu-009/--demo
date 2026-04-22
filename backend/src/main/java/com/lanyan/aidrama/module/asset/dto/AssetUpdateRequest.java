package com.lanyan.aidrama.module.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新资产请求 DTO (系分 4.3.1)
 */
@Data
@Schema(description = "更新资产请求参数")
public class AssetUpdateRequest {

    @Schema(description = "资产名称")
    private String name;

    @Schema(description = "AI描述文本")
    private String description;

    @Schema(description = "参考图URL数组JSON字符串")
    private String referenceImages;

    @Schema(description = "风格预设JSON")
    private String stylePreset;
}
