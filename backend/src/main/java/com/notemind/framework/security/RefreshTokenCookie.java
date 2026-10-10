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
 * <p><b>为什么单独一个类</b>：让 Service 层完全不碰 HTTP
 * （见《后端开发规范》"Service 层禁止出现 Web 概念"）。
 * Service 只产出 token 字符串，由 Controller 调本类写 Cookie。
 *
 * <h3>Cookie 属性（每一项都有理由）</h3>
 * <ul>
 *   <li><b>HttpOnly</b> —— JavaScript 读不到。这是整个双 Token 方案的安全基石：
 *       即使发生 XSS，攻击者也拿不到长期凭据</li>
 *   <li><b>Secure</b> —— 只在 HTTPS 下发送（生产必须开，开发走 localhost 时关闭）</li>
 *   <li><b>SameSite=Lax</b> —— 跨站 POST 不会携带它，因此 CSRF 被挡住。
 *       这是本方案<b>不需要额外 CSRF token</b> 的依据</li>
 *   <li><b>Path=/api/auth</b> —— 只有认证相关请求才携带它。
 *       若设为 {@code /}，每个业务请求都会白白带上长期凭据，暴露面大得多</li>
 * </ul>
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
