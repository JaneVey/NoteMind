package com.notemind.auth.dto;

import java.time.Duration;

/**
 * 认证操作的完整结果：**响应体内容 + 待写入 Cookie 的 refresh token**。
 *
 * <p><b>为什么需要这个组合对象</b>：登录/刷新的产物分两部分 ——
 * access token 与用户信息进响应体，refresh token 进 HttpOnly Cookie。
 * 而 Service 层不碰 HTTP，所以它必须把"要写进 Cookie 的那个字符串"一并交出来，
 * 由 Controller 负责写。
 *
 * <p>把它单独定义出来（而不是让 Controller 分别调两个方法），
 * 是为了避免"签发了凭据但忘了写 Cookie"这类两处状态不一致的 bug。
 *
 * @param body            响应体内容
 * @param refreshToken    Refresh Token 明文，<b>仅用于写 Cookie，禁止记日志或进响应体</b>
 * @param refreshTtl      决定 Cookie 的 Max-Age
 */
public record AuthResult(AuthResponse body, String refreshToken, Duration refreshTtl) {
}
