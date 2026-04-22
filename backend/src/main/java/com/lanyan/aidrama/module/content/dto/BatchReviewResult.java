package com.lanyan.aidrama.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 批量审核结果 DTO (系分 4.4.1)
 */
@Data
@Schema(description = "批量审核结果")
public class BatchReviewResult {

    @Schema(description = "总处理数量")
    private int totalCount;

    @Schema(description = "成功数量")
    private int successCount;

    @Schema(description = "失败数量")
    private int failedCount;

    @Schema(description = "失败明细列表")
    private java.util.List<FailedDetail> failedDetails;

    @Data
    @Schema(description = "失败明细")
    public static class FailedDetail {

        @Schema(description = "分镜ID")
        private Long shotId;

        @Schema(description = "失败原因")
        private String reason;
    }
}
