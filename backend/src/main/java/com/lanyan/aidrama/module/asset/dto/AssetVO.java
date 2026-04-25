package com.lanyan.aidrama.module.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产视图对象 VO (系分 v1.2 第 7.4 节)
 */
@Data
@Schema(description = "资产信息视图")
public class AssetVO {

    @Schema(description = "资产ID")
    private Long id;

    @Schema(description = "所属项目ID")
    private Long projectId;

    @Schema(description = "资产类型: character/scene/prop/voice")
    private String assetType;

    @Schema(description = "资产名称")
    private String name;

    @Schema(description = "AI描述文本")
    private String description;

    @Schema(description = "参考图URL数组JSON")
    private String referenceImages;

    @Schema(description = "父资产ID数组(JSON)")
    private String parentIds;

    @Schema(description = "是否子资产: 0-否 1-是")
    private Integer isSubAsset;

    @Schema(description = "状态: draft/confirmed/deprecated")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
