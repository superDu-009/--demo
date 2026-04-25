package com.lanyan.aidrama.module.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分镜引用视图对象
 */
@Data
@Schema(description = "资产被分镜引用信息")
public class ShotReferenceVO {

    @Schema(description = "分镜ID")
    private Long shotId;

    @Schema(description = "所属分集ID")
    private Long episodeId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
