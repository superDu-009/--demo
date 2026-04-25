package com.lanyan.aidrama.module.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * TOS 预签名上传请求
 */
@Data
@Schema(description = "TOS 预签名上传请求")
public class TosPresignRequest {

    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @Schema(description = "文件类型(MIME)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件类型不能为空")
    private String contentType;

    @Schema(description = "来源标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "来源标识不能为空")
    private String source;

    @Schema(description = "业务ID")
    private String businessId;
}
