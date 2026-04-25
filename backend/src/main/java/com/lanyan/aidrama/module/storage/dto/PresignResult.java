package com.lanyan.aidrama.module.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * TOS 预签名上传响应
 */
@Data
@Schema(description = "TOS 预签名上传响应")
public class PresignResult {

    @Schema(description = "预签名 PUT 上传 URL")
    private String uploadUrl;

    @Schema(description = "文件 Key（上传完成后保存用）")
    private String fileKey;
}
