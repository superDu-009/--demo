package com.lanyan.aidrama.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import jakarta.servlet.http.HttpServletResponse;
import com.lanyan.aidrama.common.exception.TosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 (系分 2.3)
 * 统一处理各类异常，按照错误码范围返回对应 HTTP 状态码：
 *   - 401xx → HTTP 401 (未授权)
 *   - 403xx → HTTP 403 (禁止访问)
 *   - 其他业务异常 → HTTP 200 + 业务码
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        log.warn("未登录或登录态失效: type={}", e.getType());
        if (NotLoginException.TOKEN_TIMEOUT.equals(e.getType())) {
            return Result.fail(ErrorCode.TOKEN_EXPIRED);
        }
        return Result.fail(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        log.warn("无权限访问: permission={}", e.getPermission());
        return Result.fail(ErrorCode.FORBIDDEN);
    }

    /**
     * TOS异常处理
     */
    @ExceptionHandler(TosException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleTosException(TosException e) {
        log.error("TOS异常: code={}, message={}", e.getErrorCode(), e.getMessage());
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    /**
     * 业务异常处理
     * 根据错误码范围决定 HTTP 状态码
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        int code = e.getCode();

        // 鉴权相关错误返回对应 HTTP 状态码
        if (code >= 40100 && code < 40200) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (code >= 40300 && code < 40400) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        } else {
            // 其他业务错误统一返回 HTTP 200 + 业务码
            response.setStatus(HttpStatus.OK.value());
        }

        log.warn("业务异常: code={}, message={}", code, e.getMessage());
        return Result.fail(code, e.getMessage());
    }

    /**
     * 参数校验异常处理 (@Valid/@Validated)
     * 返回 40002 字段校验失败，聚合所有字段错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(ErrorCode.VALIDATION_FAIL.getCode(), message);
    }

    /**
     * 绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("绑定异常: {}", message);
        return Result.fail(ErrorCode.VALIDATION_FAIL.getCode(), message);
    }

    /**
     * 缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(ErrorCode.PARAM_ERROR);
    }

    /**
     * HTTP 方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("HTTP 方法不支持: {}", e.getMethod());
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "不支持的请求方法");
    }

    /**
     * 资源不存在异常 (404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return Result.fail(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 未知异常兜底处理
     * 返回 50000 服务器内部错误
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }
}
