package com.lanyan.aidrama.common;

import lombok.Getter;

/**
 * 错误码规范 (系分 2.2)
 * 错误码范围与模块对应关系：
 *   0       - 成功
 *   40000-40099  - 参数校验
 *   40100-40199  - 认证鉴权
 *   40300-40399  - 权限不足
 *   40400-40499  - 资源不存在
 *   40900-40999  - 业务冲突
 *   42900-42999  - 频率限制
 *   50000-50099  - 系统异常
 *   51000-51099  - AI服务异常
 *   51100-51199  - 存储异常
 */
@Getter
public enum ErrorCode {

    // ============ 成功 ============
    SUCCESS(0, "success"),

    // ============ 参数校验 40000-40099 ============
    PARAM_ERROR(40000, "参数校验失败"),
    LOGIN_FAIL(40001, "用户名或密码错误"),
    VALIDATION_FAIL(40002, "字段校验失败"),
    FILE_TYPE_NOT_SUPPORTED(40003, "文件类型不支持"),
    FILE_SIZE_EXCEEDED(40004, "文件大小超限"),
    PRESIGN_URL_EXPIRED(40005, "预签名 URL 已过期"),

    // ============ 认证鉴权 40100-40199 ============
    UNAUTHORIZED(40100, "未登录"),
    TOKEN_EXPIRED(40101, "Token 过期"),

    // ============ 权限不足 40300-40399 ============
    FORBIDDEN(40300, "无操作权限"),
    NOT_PROJECT_OWNER(40301, "非项目创建者无权操作"),

    // ============ 资源不存在 40400-40499 ============
    RESOURCE_NOT_FOUND(40400, "资源不存在"),

    // ============ 业务冲突 40900-40999 ============
    PROJECT_EXECUTING(40900, "项目正在执行中"),
    ASSET_REFERENCED(40901, "资产已被分镜引用，不可删除"),
    SHOT_STATUS_NOT_SUPPORT(40902, "分镜状态不支持当前操作"),
    DATA_CONCURRENT_MODIFIED(40903, "数据已被修改，请刷新后重试"),

    // ============ 频率限制 42900-42999 ============
    RATE_LIMITED(42900, "请求频率过高"),

    // ============ 系统异常 50000-50099 ============
    INTERNAL_ERROR(50000, "服务器内部错误"),

    // ============ AI服务异常 51000-51099 ============
    AI_CALL_FAIL(51000, "AI 调用失败"),
    AI_TIMEOUT(51001, "AI 模型超时/排队中"),
    AI_RESULT_INVALID(51002, "AI 生成内容为空/不合格"),

    // ============ 存储异常 51100-51199 ============
    TOS_UPLOAD_FAIL(51100, "TOS 上传失败"),
    TOS_STORAGE_FULL(51101, "TOS 存储空间不足");

    /** 错误码 */
    private final int code;

    /** 错误描述 */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
