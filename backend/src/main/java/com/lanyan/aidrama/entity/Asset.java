package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产实体类 (对应 asset 表，系分 v1.2 第 5.5 节)
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

    /** 参考图URL数组(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String referenceImages;

    /** 父资产ID数组(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String parentIds;

    /** 是否子资产: 0-否 1-是 */
    private Integer isSubAsset;

    /** 草稿内容(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String draftContent;

    /** 状态: draft/confirmed/deprecated */
    private String status;

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
