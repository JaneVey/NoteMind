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
 * <p>Access Token 走响应体，Refresh Token 走 HttpOnly Cookie —— 后者 JS 读不到。
 * 因此 CSRF 防护依赖 Cookie 的 SameSite 不被降级为 None。
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
     * 没有请求体 —— refresh token 只在 Cookie 里。
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

    /** 登出。幂等：Cookie 不存在或已失效也返回成功，并清除 Cookie */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        authService.logout(refreshTokenCookie.read(httpRequest));
        refreshTokenCookie.clear(httpResponse);
        return Result.success();
    }
}
