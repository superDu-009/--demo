package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产实体类 (对应 asset 表，系分 3. DDL 第3张表)
 * 支持多参考图（reference_images JSON数组）
 */
@Data
@TableName(value = "asset", autoResultMap = true)
public class Asset {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 资产类型: character/scene/prop/voice */
    private String assetType;

    /** 资产名称 */
    private String name;

    /** AI描述文本 */
    private String description;

    /** 参考图URL数组, 第一个为主图 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String referenceImages;

    /** 风格预设(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String stylePreset;

    /** 状态: 0-草稿 1-已确认 2-已废弃 */
    private Integer status;

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
