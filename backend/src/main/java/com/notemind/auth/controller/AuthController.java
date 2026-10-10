package com.notemind.auth.controller;

import com.notemind.auth.dto.AuthResponse;
import com.notemind.auth.dto.AuthResult;
import com.notemind.auth.dto.LoginRequest;
import com.notemind.auth.dto.RegisterRequest;
import com.notemind.auth.service.AuthService;
import com.notemind.common.result.Result;
import com.notemind.framework.security.RefreshTokenCookie;
import com.notemind.framework.security.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * <p><b>本类的职责</b>：把 Service 产出的 refresh token 写进 HttpOnly Cookie，
 * 把 access token 与用户信息放进响应体。业务规则全在 {@link AuthService}。
 *
 * <h3>双 Token 的传输方式（这是安全设计的核心）</h3>
 * <pre>
 *   Access Token   → 响应体 JSON          前端只放内存（不进 localStorage）
 *   Refresh Token  → HttpOnly Cookie       JavaScript 读不到
 * </pre>
 *
 * <p>因此即使发生 XSS，攻击者也只能拿到 30 分钟的 access token，
 * 而拿不到可以长期续期的 refresh token。
 *
 * <p><b>为什么不需要额外的 CSRF token</b>：{@code /refresh} 与 {@code /logout} 是 POST，
 * 而 Refresh Cookie 的 {@code SameSite=Lax} <b>不会随跨站 POST 发送</b>；
 * 且攻击者跨站也读不到响应体。这个结论成立的前提是 SameSite 不被降级为 None。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse httpResponse) {
        AuthResult result = authService.login(
                request, RequestUtils.userAgent(httpRequest), RequestUtils.clientIp(httpRequest));

        refreshTokenCookie.write(httpResponse, result.refreshToken(), result.refreshTtl());
        return Result.success(result.body());
    }

    /**
     * 用 Cookie 中的 refresh token 换取新的 access token。
     *
     * <p>前端在三处调用它：① 页面启动时恢复登录态；
     * ② access token 过期收到 401 时（需用 single-flight 避免并发重复刷新）；
     * ③ OAuth 回调跳回后。
     *
     * <p>没有请求体 —— refresh token 只在 Cookie 里。
     */
    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse) {
        AuthResult result = authService.refresh(
                refreshTokenCookie.read(httpRequest),
                RequestUtils.userAgent(httpRequest),
                RequestUtils.clientIp(httpRequest));

        // 轮换后的新 refresh token 覆盖旧 Cookie
        refreshTokenCookie.write(httpResponse, result.refreshToken(), result.refreshTtl());
        return Result.success(result.body());
    }

    /**
     * 登出。
     *
     * <p>与旧实现的关键区别：**服务端会真正吊销这个 refresh token**，
     * 而不只是让前端"忘记"它。这也是本次认证重设计要解决的核心问题之一。
     *
     * <p>幂等：即使 Cookie 不存在或已失效，也返回成功并清除 Cookie。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        authService.logout(refreshTokenCookie.read(httpRequest));
        refreshTokenCookie.clear(httpResponse);
        return Result.success();
    }
}
