package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目视图对象 VO (系分 v1.2 第 7.2 节)
 */
@Data
@Schema(description = "项目信息视图")
public class ProjectVO {

    @Schema(description = "项目ID")
    private Long id;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "原始小说文件TOS路径")
    private String novelOriginalTosPath;

    @Schema(description = "解析后纯文本TOS路径")
    private String novelTosPath;

    @Schema(description = "画面比例")
    private String ratio;

    @Schema(description = "清晰度")
    private String definition;

    @Schema(description = "风格")
    private String style;

    @Schema(description = "风格描述")
    private String styleDesc;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
