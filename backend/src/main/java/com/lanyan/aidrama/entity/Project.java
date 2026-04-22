package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目实体类 (对应 project 表，系分 3. DDL 第2张表)
 * 包含 workflow_config 和 style_preset JSON 字段
 */
@Data
@TableName(value = "project", autoResultMap = true)
public class Project {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建人ID */
    private Long userId;

    /** 项目名称 */
    private String name;

    /** 项目描述 */
    private String description;

    /** 小说文件TOS路径 */
    private String novelTosPath;

    /** 流程配置(JSON数组) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String workflowConfig;

    /** 全局风格预设(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String stylePreset;

    /** 状态: 0-草稿 1-进行中 2-已完成 */
    private Integer status;

    /** 执行锁: 0-未执行 1-执行中 */
    private Integer executionLock;

    /** 乐观锁版本号 (系分 v1.2 新增) */
    @Version
    private Integer version;

    /** 逻辑删除: 0-正常 1-删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
