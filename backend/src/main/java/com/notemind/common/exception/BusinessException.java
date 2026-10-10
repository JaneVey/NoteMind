package com.notemind.common.exception;

/**
 * 业务异常。
 *
 * <p>必须携带 {@link ErrorCode}，不接受裸数字码 —— 每处失败都要有明确的业务归属，
 * 否则前端拿到的全是一个笼统的 400。
 *
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
