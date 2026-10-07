package com.notemind.common.exception;

import com.notemind.common.result.Result;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p><b>变更记录（2026-10-07）——统一 HTTP 状态码与业务 code</b>
 *
 * <p>原实现混用了两种风格：{@code BusinessException} 不带 {@code @ResponseStatus}，
 * 因此登录失败返回的是 <b>HTTP 200 + body 中 code=401</b>；
 * 而 {@code UnauthorizedException} 等又带 {@code @ResponseStatus}，返回 HTTP 401。
 * 同一个语义出现两种 HTTP 状态码，会让前端拦截器、日志与监控都难以判断。
 *
 * <p>现统一为：<b>HTTP 状态码由业务 code 映射得出，两者始终一致</b>。
 * 响应体仍保持 {@code Result{code, message, data}} 结构不变，
 * 因为"统一响应壳"是这个项目的既定约定。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={} message={}", e.getCode(), e.getMessage());
        return build(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<Void>> handleUnauthorized(UnauthorizedException e) {
        return build(401, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentials(BadCredentialsException e) {
        return build(401, "用户名或密码错误");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Result<Void>> handleJwtException(JwtException e) {
        return build(401, "Token 无效或已过期");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return build(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return build(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return build(500, "服务器内部错误");
    }

    /**
     * 按业务 code 映射 HTTP 状态码。无法识别的 code 一律回落为 500。
     */
    private ResponseEntity<Result<Void>> build(int code, String message) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(Result.error(code, message));
    }
}
