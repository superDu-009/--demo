package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分集视图对象 VO (系分 v1.2 第 7.3 节)
 */
@Data
@Schema(description = "分集信息视图")
public class EpisodeVO {

    @Schema(description = "分集ID")
    private Long id;

    @Schema(description = "所属项目ID")
    private Long projectId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "分集摘要")
    private String summary;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "分集剧本内容")
    private String content;

    @Schema(description = "解析状态: pending/analyzing/success/failed")
    private String parseStatus;

    @Schema(description = "解析失败原因")
    private String parseError;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
