package com.notemind.auth.token;

/**
 * 一次刷新会话的上下文。
 *
 * <p>rememberMe 必须存下来并在轮换时沿用：{@code /api/auth/refresh} 没有请求体
 * （refresh token 在 Cookie 里），不然每次刷新都会按默认有效期重算，"记住我"等于失效。
 *
 * @param userId     会话归属用户
 * @param rememberMe 登录时是否勾选"记住我"
 */
public record RefreshSession(Long userId, boolean rememberMe) {
}
