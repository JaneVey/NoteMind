package com.notemind.framework.security;

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
 */
@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("未认证访问: {} {}", request.getMethod(), request.getRequestURI());
        SecurityResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, 401,
                "未登录或登录已过期");
    }
}
