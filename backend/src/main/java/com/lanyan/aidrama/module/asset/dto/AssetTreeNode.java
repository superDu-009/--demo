package com.lanyan.aidrama.module.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 资产关系树节点
 */
@Data
@Schema(description = "资产树节点")
public class AssetTreeNode {

    @Schema(description = "资产ID")
    private Long id;

    @Schema(description = "资产名称")
    private String name;

    @Schema(description = "资产类型")
    private String assetType;

    @Schema(description = "是否子资产")
    private Boolean isSubAsset;

    @Schema(description = "子节点")
    private List<AssetTreeNode> children;
}
