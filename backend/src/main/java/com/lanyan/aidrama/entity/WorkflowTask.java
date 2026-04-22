package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程任务实体类 (对应 workflow_task 表，系分 3. DDL 第8张表)
 * 支持 sub_step 断点续跑
 */
@Data
@TableName(value = "workflow_task", autoResultMap = true)
public class WorkflowTask {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 当前处理的分集ID(可为空) */
    private Long episodeId;

    /** 步骤类型: import/asset_extract/shot_gen/image_gen/video_gen/export */
    private String stepType;

    /** 步骤顺序 */
    private Integer stepOrder;

    /** 状态: 0-未执行 1-执行中 2-成功 3-失败 4-待审核 */
    private Integer status;

    /** 子步骤: submit/polling/download/upload_tos(用于断点续跑) */
    private String subStep;

    /** 输入数据(JSON) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String inputData;

    /** 输出数据(JSON), 保存中间结果 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String outputData;

    /** 审核意见 */
    private String reviewComment;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
