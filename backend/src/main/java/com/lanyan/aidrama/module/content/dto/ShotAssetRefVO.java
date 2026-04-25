package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分镜资产关联视图对象
 */
@Data
@Schema(description = "分镜资产关联信息")
public class ShotAssetRefVO {

    @Schema(description = "资产ID")
    private Long assetId;

    @Schema(description = "资产类型")
    private String assetType;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "资产主参考图URL")
    private String primaryImage;
}
