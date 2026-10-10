package com.notemind.auth.token;

/**
 * 一次刷新会话的上下文。
 *
 * <p>{@code rememberMe} 必须存下来并在轮换时沿用 ——
 * 因为 {@code /api/auth/refresh} 请求<b>没有 body</b>（refresh token 在 Cookie 里），
 * 无法从请求中得知用户当初是否勾选了"记住我"。
 * 若不保存，每次刷新都会按默认值重新计算有效期，等于"记住我"在第一次刷新后就失效了。
 *
 * @param userId     会话归属用户
 * @param rememberMe 登录时是否勾选"记住我"
 */
public record RefreshSession(Long userId, boolean rememberMe) {
}
