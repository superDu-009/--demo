package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分镜实体类 (对应 shot 表，系分 3. DDL 第6张表)
 */
@Data
public class Shot {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分场ID */
    private Long sceneId;

    /** 排序号 */
    private Integer sortOrder;

    /** AI生图提示词(中文) */
    private String prompt;

    /** AI生图提示词(英文) */
    private String promptEn;

    /** 生成图片URL(TOS) */
    private String generatedImageUrl;

    /** 生成视频URL(TOS) */
    private String generatedVideoUrl;

    /** 状态: 0-待处理 1-生成中 2-待审核 3-已通过 4-已打回 5-已完成 */
    private Integer status;

    /** 审核意见 */
    private String reviewComment;

    /** 版本号 */
    private Integer version;

    /** 生成尝试次数 */
    private Integer generationAttempts;

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
