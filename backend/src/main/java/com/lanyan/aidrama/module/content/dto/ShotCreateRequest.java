package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分镜创建请求 DTO (系分 v1.2 第 7.3 节)
 */
@Data
@Schema(description = "创建分镜请求参数")
public class ShotCreateRequest {

    @Schema(description = "AI提示词(中文)")
    private String prompt;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "时长(秒): 10/12/15")
    private Integer duration;

    @Schema(description = "场景类型")
    private String sceneType;

    @Schema(description = "运镜方式")
    private String cameraMove;

    @Schema(description = "台词数组")
    private List<String> lines;

    @Schema(description = "是否承接上一分镜: 0-否 1-是", defaultValue = "1")
    private Integer followLast;

    @Schema(description = "绑定资产ID列表")
    private List<Long> assetIds;
}
