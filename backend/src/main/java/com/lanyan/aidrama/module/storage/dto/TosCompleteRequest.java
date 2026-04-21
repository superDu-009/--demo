package com.lanyan.aidrama.module.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * TOS上传完成回调请求DTO (系分 4.7.1)
 */
@Data
@Schema(description = "TOS上传完成通知请求参数")
public class TosCompleteRequest {
    /**
     * TOS中存储的文件唯一Key
     */
    @NotBlank(message = "文件Key不能为空")
    @Schema(description = "上传成功的文件Key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileKey;

    /**
     * 关联业务ID，如项目ID、资产ID
     */
    @NotNull(message = "关联业务ID不能为空")
    @Schema(description = "关联业务ID，如项目ID、资产ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long businessId;

    /**
     * 文件大小（单位：字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Schema(description = "文件大小（单位：字节）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileSize;

    /**
     * 原始文件名
     */
    @NotBlank(message = "原始文件名不能为空")
    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalName;
}
