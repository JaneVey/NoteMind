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
 * <p>不配置它时 Spring Security 默认返回 403，前端无法区分"未登录"与"无权限"。
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
