package com.lanyan.aidrama.common;

import lombok.Getter;

/**
 * 业务异常类 (系分 2.2)
 * 业务逻辑中抛出此异常，由 GlobalExceptionHandler 统一捕获处理
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    /**
     * 使用 ErrorCode 枚举构造
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用自定义错误码和消息构造
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用 ErrorCode 枚举构造，并携带原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }
}
