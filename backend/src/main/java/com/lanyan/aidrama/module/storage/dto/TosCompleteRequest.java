package com.lanyan.aidrama.module.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * TOS 上传完成通知请求
 */
@Data
@Schema(description = "TOS 上传完成通知请求")
public class TosCompleteRequest {

    @Schema(description = "文件 Key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件 Key 不能为空")
    private String fileKey;

    @Schema(description = "来源标识")
    private String source;

    @Schema(description = "用户ID（自动填充，不传前端）")
    private Long userId;
}
