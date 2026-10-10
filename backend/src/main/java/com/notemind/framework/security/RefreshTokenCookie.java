package com.notemind.framework.security;

import com.notemind.framework.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 的 Cookie 读写。
 *
 * <p>Cookie 必须是 HttpOnly（JS 读不到）且 Path 限定为 {@code /api/auth}，
 * 否则每个业务请求都会白白带上长期凭据。
 *
 * <p>SameSite=Lax 是本方案不做额外 CSRF token 的依据，被降级为 None 时该结论不成立。
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {

    private final AuthProperties authProperties;

    /** 从请求中读取 refresh token，不存在返回 {@code null} */
    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String name = authProperties.getRefreshToken().getCookieName();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** 写入（或覆盖）refresh token Cookie */
    public void write(HttpServletResponse response, String value, Duration ttl) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(value, ttl).toString());
    }

    /** 清除 Cookie（登出时）。Max-Age=0 让浏览器立即丢弃 */
    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    private ResponseCookie build(String value, Duration maxAge) {
        AuthProperties.RefreshToken cfg = authProperties.getRefreshToken();
        return ResponseCookie.from(cfg.getCookieName(), value)
                .httpOnly(true)
                .secure(cfg.isCookieSecure())
                .path(cfg.getCookiePath())
                .maxAge(maxAge)
                .sameSite(cfg.getCookieSameSite())
                .build();
    }
}
