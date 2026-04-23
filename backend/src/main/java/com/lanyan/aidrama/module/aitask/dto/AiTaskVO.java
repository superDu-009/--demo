package com.lanyan.aidrama.module.aitask.dto;

import lombok.Data;

/**
 * AI任务视图对象 (系分 4.6.2)
 */
@Data
public class AiTaskVO {

    /** 任务ID */
    private Long id;

    /** 任务类型: image_gen/video_gen */
    private String taskType;

    /** 状态: 0-提交中 1-处理中 2-成功 3-失败 */
    private Integer status;

    /** 结果URL(TOS) */
    private String resultUrl;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private java.time.LocalDateTime createTime;

    /** 更新时间 */
    private java.time.LocalDateTime updateTime;
}
