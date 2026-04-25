package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目实体类 (对应 project 表，系分 v1.2 第 5.2 节)
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

    /** 原始小说文件TOS路径 */
    private String novelOriginalTosPath;

    /** 解析后纯文本TOS路径 */
    private String novelTosPath;

    /** 画面比例: 16:9 / 9:16 */
    private String ratio;

    /** 清晰度: 720P / 1080P */
    private String definition;

    /** 风格: 2D次元风/日漫风/国漫风/古风/现代写实/自定义 */
    private String style;

    /** 风格描述（自定义风格时使用） */
    private String styleDesc;

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
