package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分镜视图对象 VO (系分 4.4.1)
 */
@Data
@Schema(description = "分镜信息视图")
public class ShotVO {

    @Schema(description = "分镜ID")
    private Long id;

    @Schema(description = "所属分场ID")
    private Long sceneId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "AI生图提示词(中文)")
    private String prompt;

    @Schema(description = "AI生图提示词(英文)")
    private String promptEn;

    @Schema(description = "生成图片URL")
    private String generatedImageUrl;

    @Schema(description = "生成视频URL")
    private String generatedVideoUrl;

    @Schema(description = "状态: 0-待处理 1-生成中 2-待审核 3-已通过 4-已打回 5-已完成")
    private Integer status;

    @Schema(description = "审核意见")
    private String reviewComment;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "生成尝试次数")
    private Integer generationAttempts;

    @Schema(description = "关联资产列表")
    private List<ShotAssetRefVO> assetRefs;

    @Schema(description = "当前最新AI任务")
    private ShotAiTaskVO currentAiTask;
}
