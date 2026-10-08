package com.notemind.common.exception;

/**
 * 业务异常。
 *
 * <p><b>变更记录（2026-10-08）</b>：改为必须携带 {@link ErrorCode}，
 * 不再允许直接传裸数字码（原先 {@code new BusinessException(400, "...")} 的写法已移除）。
 * 目的是强制每处失败都有明确的业务归属，避免"到处都是 400"，前端无法区分。
 *
 * <p>用法：
 * <pre>
 *   throw new BusinessException(ErrorCode.USERNAME_EXISTS);
 *   throw new BusinessException(ErrorCode.NOTE_NOT_FOUND, "笔记 " + id + " 不存在");
 * </pre>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 使用自定义提示文案，错误码不变 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** 业务码，供全局异常处理器填充 {@code Result.code} */
    public int getCode() {
        return errorCode.getCode();
    }
}
