package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API调用记录实体类 (对应 api_call_log 表，系分 3. DDL 第10张表)
 */
@Data
@TableName(value = "api_call_log", autoResultMap = true)
public class ApiCallLog {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 项目ID */
    private Long projectId;

    /** 提供商 */
    private String apiProvider;

    /** 接口名称 */
    private String apiEndpoint;

    /** 请求摘要(脱敏) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String requestParams;

    /** Token消耗 */
    private Integer tokenUsage;

    /** 费用(元) */
    private BigDecimal cost;

    /** 状态: 0-失败 1-成功 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
