package com.notemind.auth.dto;

import java.time.Duration;

/**
 * 认证操作的完整结果：响应体内容 + 待写入 Cookie 的 refresh token。
 *
 * <p>合成一个对象是避免"签发了凭据但忘了写 Cookie"这类两处状态不一致的 bug。
 *
 * @param body            响应体内容
 * @param refreshToken    Refresh Token 明文，仅用于写 Cookie，禁止记日志或进响应体
 * @param refreshTtl      决定 Cookie 的 Max-Age
 */
public record AuthResult(AuthResponse body, String refreshToken, Duration refreshTtl) {
}
