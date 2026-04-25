package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分镜实体类 (对应 shot 表，系分 v1.2 第 5.4 节)
 */
@Data
@TableName(value = "shot", autoResultMap = true)
public class Shot {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分集ID */
    private Long episodeId;

    /** 排序号 */
    private Integer sortOrder;

    /** AI生图提示词(中文) */
    private String prompt;

    /** AI生图提示词(英文) */
    private String promptEn;

    /** 时长(秒): 10/12/15 */
    private Integer duration;

    /** 场景类型 */
    private String sceneType;

    /** 运镜方式 */
    private String cameraMove;

    /** 台词数组(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String lines;

    /** 生成图片URL(TOS) */
    private String generatedImageUrl;

    /** 生成视频URL(TOS) */
    private String generatedVideoUrl;

    /** 尾帧图片URL */
    private String lastFrameUrl;

    /** 是否承接上一分镜: 0-否 1-是 */
    private Integer followLast;

    /** 草稿内容(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String draftContent;

    /** 提示词状态: pending/generating/success/failed */
    private String promptStatus;

    /** 图片状态: pending/generating/success/failed */
    private String imageStatus;

    /** 视频状态: pending/generating/success/failed */
    private String videoStatus;

    /** 错误信息 */
    private String errorMsg;

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
