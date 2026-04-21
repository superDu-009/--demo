package com.lanyan.aidrama.common.exception;

import lombok.Getter;

/**
 * TOS操作自定义异常
 * 错误码范围：51100-51199
 */
@Getter
public class TosException extends RuntimeException {
    /**
     * 业务错误码
     * 51100 = TOS通用操作失败
     * 51101 = 生成预签名URL失败
     * 51102 = 上传完成校验失败（文件不存在）
     * 51103 = 文件上传失败
     * 51104 = 文件删除失败
     */
    private final Integer errorCode;

    public TosException(Integer errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TosException(String message) {
        this(51100, message);
    }
}
