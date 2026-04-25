package com.lanyan.aidrama.module.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 重复资产检测结果
 */
@Data
@Schema(description = "重复资产组")
public class AssetDuplicateVO {

    @Schema(description = "资产ID列表")
    private List<Long> assetIds;

    @Schema(description = "资产名称列表")
    private List<String> assetNames;

    @Schema(description = "相似度百分比")
    private Double similarity;
}
