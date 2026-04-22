package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分镜-资产引用 DTO，用于分镜列表中展示关联资产
 */
@Data
@Schema(description = "分镜-资产引用")
public class ShotAssetRefVO {

    @Schema(description = "资产ID")
    private Long assetId;

    @Schema(description = "资产类型: character/scene/prop")
    private String assetType;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "资产主图URL")
    private String primaryImage;
}
