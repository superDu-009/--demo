package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Prompt配置实体类 (对应 prompt_config 表，系分 v1.2 第 5.8 节)
 */
@Data
@TableName("prompt_config")
public class PromptConfig {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Prompt标识 */
    private String promptKey;

    /** Prompt内容 */
    private String promptText;

    /** 适用模型 */
    private String model;

    /** 描述 */
    private String description;

    /** 版本号 */
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
