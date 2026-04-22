package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI任务实体类 (对应 ai_task 表，系分 3. DDL 第9张表)
 * 支持指数退避轮询
 */
@Data
public class AiTask {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** 关联分镜ID */
    private Long shotId;

    /** 任务类型: image_gen/video_gen */
    private String taskType;

    /** 第三方API返回的任务ID */
    private String providerTaskId;

    /** 状态: 0-提交中 1-处理中 2-成功 3-失败 */
    private Integer status;

    /** 结果URL(TOS) */
    private String resultUrl;

    /** 错误信息 */
    private String errorMsg;

    /** 下次轮询时间(指数退避) */
    private LocalDateTime nextPollTime;

    /** 已轮询次数 */
    private Integer pollCount;

    /** 上次轮询时间 */
    private LocalDateTime lastPollTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
