package com.notemind.auth.token;

import java.time.Duration;

/**
 * 一次签发产生的凭据对。
 *
 * <p><b>为什么 refresh token 要单独返回而不是直接塞进响应体</b>：
 * {@code AuthResponse}（响应体）里只有 access token；
 * refresh token 由 Controller 写入 <b>HttpOnly Cookie</b> ——
 * 这样 JavaScript 读不到它，即使发生 XSS 也拿不到长期凭据。
 * 所以这个内部对象只在 Service → Controller 之间传递，不出现在对外 JSON 中。
 *
 * @param accessToken  Access Token（JWT）
 * @param expiresIn    Access Token 有效秒数
 * @param refreshToken Refresh Token **明文**（不透明随机串）—— 仅用于写 Cookie，禁止记日志
 * @param refreshTtl   Refresh Token 有效期（决定 Cookie 的 Max-Age）
 */
public record IssuedTokens(
        String accessToken,
        long expiresIn,
        String refreshToken,
        Duration refreshTtl
) {
}
