package com.lanyan.aidrama.module.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量任务状态查询请求
 */
@Data
@Schema(description = "批量任务状态查询请求")
public class BatchTaskStatusRequest {

    @Schema(description = "任务ID列表（与batchId二选一）")
    private List<Long> taskIds;

    @Schema(description = "批量ID（与taskIds二选一）")
    private String batchId;
}
