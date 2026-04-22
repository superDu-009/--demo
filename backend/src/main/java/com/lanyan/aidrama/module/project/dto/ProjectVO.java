package com.lanyan.aidrama.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目视图对象 VO (系分 4.2.1)
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

    @Schema(description = "小说文件TOS路径")
    private String novelTosPath;

    @Schema(description = "流程配置JSON")
    private String workflowConfig;

    @Schema(description = "风格预设JSON")
    private String stylePreset;

    @Schema(description = "状态: 0-草稿 1-进行中 2-已完成")
    private Integer status;

    @Schema(description = "执行锁: 0-未执行 1-执行中")
    private Integer executionLock;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
