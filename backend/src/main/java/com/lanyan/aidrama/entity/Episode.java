package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分集实体类 (对应 episode 表，系分 3. DDL 第4张表)
 */
@Data
public class Episode {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 标题 */
    private String title;

    /** 排序号 */
    private Integer sortOrder;

    /** 分集剧本内容 */
    private String content;

    /** 状态: 0-待处理 1-进行中 2-已完成 */
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
