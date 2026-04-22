package com.lanyan.aidrama.module.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分镜引用视图 (系分 4.3.1 资产引用查询)
 */
@Data
@Schema(description = "分镜引用信息")
public class ShotReferenceVO {

    @Schema(description = "分镜ID")
    private Long shotId;

    @Schema(description = "所属分场ID")
    private Long sceneId;

    @Schema(description = "所属分集ID")
    private Long episodeId;

    @Schema(description = "分镜状态")
    private Integer shotStatus;
}
