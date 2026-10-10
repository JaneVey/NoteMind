package com.notemind.common.exception;

import com.notemind.common.result.Result;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：把各层抛出的异常统一转成「HTTP 状态码 + 业务码 + 文案」的响应。
 *
 * <p>日志级别：业务异常（可预期的用户错误）记 WARN，系统异常记 ERROR 且必须带堆栈。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={} message={}", e.getCode(), e.getMessage());
        return build(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<Void>> handleUnauthorized(UnauthorizedException e) {
        return build(ErrorCode.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentials(BadCredentialsException e) {
        return build(ErrorCode.LOGIN_FAILED);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Result<Void>> handleJwtException(JwtException e) {
        log.debug("Token 解析失败: {}", e.getMessage());
        return build(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return build(ErrorCode.PARAM_INVALID, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        return build(ErrorCode.PARAM_INVALID, detail);
    }

    /**
     * 路径不存在。
     *
     * <p>必须单独处理：否则会被 {@code Exception} 兜底捕获，把客户端请求错地址报成 500。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        log.debug("路径不存在: {}", e.getResourcePath());
        return build(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /** 请求方法不支持（如用 GET 调 POST 接口） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.debug("请求方法不支持: {}", e.getMessage());
        return build(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /** 请求体不是合法 JSON 或与目标类型不匹配 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return build(ErrorCode.PARAM_INVALID, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        // 未预期的异常：必须带堆栈，否则无法定位
        log.error("系统异常", e);
        return build(ErrorCode.SYSTEM_ERROR);
    }

    private ResponseEntity<Result<Void>> build(ErrorCode errorCode) {
        return build(errorCode, errorCode.getMessage());
    }

    private ResponseEntity<Result<Void>> build(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(Result.error(errorCode.getCode(), message));
    }
}
