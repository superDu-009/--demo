package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分场视图对象 VO
 */
@Data
@Schema(description = "分场信息视图")
public class SceneVO {

    @Schema(description = "分场ID")
    private Long id;

    @Schema(description = "所属分集ID")
    private Long episodeId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "分场描述")
    private String content;

    @Schema(description = "状态: 0-待处理 1-进行中 2-已完成")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
