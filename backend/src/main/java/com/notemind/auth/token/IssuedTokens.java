package com.notemind.auth.token;

import java.time.Duration;

/**
 * 一次签发产生的凭据对（Service → Controller 之间的内部对象，不出现在对外 JSON 中）。
 *
 * @param accessToken  Access Token（JWT）
 * @param expiresIn    Access Token 有效秒数
 * @param refreshToken Refresh Token 明文（不透明随机串）—— 仅用于写 Cookie，禁止记日志
 * @param refreshTtl   Refresh Token 有效期（决定 Cookie 的 Max-Age）
 */
public record IssuedTokens(
        String accessToken,
        long expiresIn,
        String refreshToken,
        Duration refreshTtl
) {
}
