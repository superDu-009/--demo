package com.lanyan.aidrama.module.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 获取预签名上传URL请求DTO (系分 4.7.1)
 */
@Data
@Schema(description = "获取预签名上传URL请求参数")
public class TosPresignRequest {

    /**
     * 原始文件名
     */
    @NotBlank(message = "文件名不能为空")
    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    /**
     * 文件MIME类型，如 image/png、video/mp4
     */
    @NotBlank(message = "文件MIME类型不能为空")
    @Schema(description = "文件MIME类型，如 image/png、video/mp4", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentType;

    /**
     * 上传来源：frontend-前端上传 / backend-后端内部上传
     */
    @NotNull(message = "上传来源不能为空")
    @Schema(description = "上传来源：frontend-前端上传 / backend-后端内部上传", requiredMode = Schema.RequiredMode.REQUIRED)
    private UploadSource source;

    /**
     * 关联业务ID，如项目ID、资产ID
     */
    @NotNull(message = "关联业务ID不能为空")
    @Schema(description = "关联业务ID，如项目ID、资产ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long businessId;

    /**
     * 上传来源枚举
     */
    public enum UploadSource {
        /** 前端上传 */
        frontend,
        /** 后端内部上传 */
        backend
    }
}
