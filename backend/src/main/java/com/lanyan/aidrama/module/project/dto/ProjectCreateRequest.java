package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目创建请求 DTO (系分 4.2.1)
 */
@Data
@Schema(description = "创建项目请求参数")
public class ProjectCreateRequest {

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "小说文件TOS路径")
    private String novelTosPath;
}
