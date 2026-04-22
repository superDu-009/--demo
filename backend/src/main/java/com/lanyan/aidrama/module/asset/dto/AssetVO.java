package com.lanyan.aidrama.module.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资产视图对象 VO (系分 4.3.1)
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

    @Schema(description = "风格预设JSON")
    private String stylePreset;

    @Schema(description = "状态: 0-草稿 1-已确认 2-已废弃")
    private Integer status;

    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;

    @Schema(description = "更新时间")
    private java.time.LocalDateTime updateTime;
}
