package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分集更新请求 DTO
 */
@Data
@Schema(description = "更新分集请求参数")
public class EpisodeUpdateRequest {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "分集剧本内容")
    private String content;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
