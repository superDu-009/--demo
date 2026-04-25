package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一任务实体类 (对应 task 表，系分 v1.2 第 5.7 节)
 * 替代旧 ai_task 表，统一管理所有异步任务
 */
@Data
@TableName(value = "task", autoResultMap = true)
public class Task {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务类型: script_analyze/shot_split/asset_extract/prompt_gen/image_gen/video_gen */
    private String type;

    /** 所属项目ID */
    private Long projectId;

    /** 所属分集ID */
    private Long episodeId;

    /** 所属分镜ID */
    private Long shotId;

    /** 批量ID（UUID） */
    private String batchId;

    /** 第三方API返回的任务ID */
    private String providerTaskId;

    /** 输入数据(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String inputData;

    /** 结果数据(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String resultData;

    /** 状态: 0-待处理 1-处理中 2-成功 3-失败 */
    private Integer status;

    /** 结果URL */
    private String resultUrl;

    /** 错误信息 */
    private String errorMsg;

    /** 进度百分比 0-100 */
    private Integer progress;

    /** 下次轮询时间 */
    private LocalDateTime nextPollTime;

    /** 已轮询次数 */
    private Integer pollCount;

    /** 上次轮询时间 */
    private LocalDateTime lastPollTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
