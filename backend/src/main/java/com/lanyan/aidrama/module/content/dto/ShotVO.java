package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分镜视图对象 VO (系分 v1.2 第 7.3 节)
 */
@Data
@Schema(description = "分镜信息视图")
public class ShotVO {

    @Schema(description = "分镜ID")
    private Long id;

    @Schema(description = "所属分集ID")
    private Long episodeId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "AI提示词(中文)")
    private String prompt;

    @Schema(description = "AI提示词(英文)")
    private String promptEn;

    @Schema(description = "时长(秒): 10/12/15")
    private Integer duration;

    @Schema(description = "场景类型")
    private String sceneType;

    @Schema(description = "运镜方式")
    private String cameraMove;

    @Schema(description = "生成图片URL")
    private String generatedImageUrl;

    @Schema(description = "生成视频URL")
    private String generatedVideoUrl;

    @Schema(description = "尾帧图片URL")
    private String lastFrameUrl;

    @Schema(description = "是否承接上一分镜: 0-否 1-是")
    private Integer followLast;

    @Schema(description = "草稿内容")
    private String draftContent;

    @Schema(description = "提示词状态: pending/generating/success/failed")
    private String promptStatus;

    @Schema(description = "图片状态: pending/generating/success/failed")
    private String imageStatus;

    @Schema(description = "视频状态: pending/generating/success/failed")
    private String videoStatus;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "绑定的资产列表")
    private List<ShotAssetRefVO> assetRefs;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
