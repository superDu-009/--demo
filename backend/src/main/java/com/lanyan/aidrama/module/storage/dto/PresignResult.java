package com.lanyan.aidrama.module.storage.dto;

import lombok.Data;

/**
 * 预签名URL返回结果DTO
 */
@Data
public class PresignResult {
    /**
     * 预签名PUT上传URL
     */
    private String uploadUrl;

    /**
     * 文件在TOS中的唯一存储Key
     */
    private String fileKey;

    /**
     * URL有效时长（单位：秒）
     */
    private Long expireSeconds;
}
