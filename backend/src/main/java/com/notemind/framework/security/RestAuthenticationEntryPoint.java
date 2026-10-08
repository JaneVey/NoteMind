package com.notemind.framework.security;

import com.notemind.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证访问受保护接口时的响应（401）。
 *
 * <p>Spring Security 默认在没有配置 EntryPoint 时返回 <b>403</b>，
 * 这会让前端无法区分"未登录"与"无权限"。
 * 本项目为无状态 JWT，前端只需要在收到 401 时清除 Token 并跳转登录页。
 *
 * <p><b>变更记录（2026-10-08）</b>：业务码改用 {@link ErrorCode#UNAUTHORIZED}（1002）。
 * 此前这里手写的是 HTTP 状态码 401，导致同一个"未登录"语义出现两种业务码
 * （安全层给 401，业务层给 1002），前端与日志都无法统一判断。
 */
@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("未认证访问: {} {}", request.getMethod(), request.getRequestURI());
        SecurityResponseWriter.write(response,
                ErrorCode.UNAUTHORIZED.getHttpStatus(),
                ErrorCode.UNAUTHORIZED.getCode(),
                ErrorCode.UNAUTHORIZED.getMessage());
    }
}
