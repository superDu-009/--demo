package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目更新请求 DTO (系分 4.2.1)
 */
@Data
@Schema(description = "更新项目请求参数")
public class ProjectUpdateRequest {

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "小说文件TOS路径")
    private String novelTosPath;

    @Schema(description = "状态: 0-草稿 1-进行中 2-已完成")
    private Integer status;
}
