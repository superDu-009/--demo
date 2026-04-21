package com.lanyan.aidrama.common;

import lombok.Data;

/**
 * 统一响应格式 (系分 2.1)
 * 所有接口返回统一使用 Result<T> 包装
 * code=0 表示成功，非 0 表示失败
 */
@Data
public class Result<T> {

    /** 业务状态码，0 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 时间戳 */
    private long timestamp;

    /**
     * 成功响应
     */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMessage("success");
        r.setData(data);
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }

    /**
     * 失败响应（使用 ErrorCode）
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        Result<T> r = new Result<>();
        r.setCode(errorCode.getCode());
        r.setMessage(errorCode.getMessage());
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }

    /**
     * 失败响应（自定义错误码和消息）
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }
}
