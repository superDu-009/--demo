package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量操作返回结果
 */
@Data
@Schema(description = "批量操作响应")
public class BatchResultVO {

    @Schema(description = "批量ID（UUID）")
    private String batchId;

    @Schema(description = "任务ID列表")
    private List<Long> taskIds;
}
