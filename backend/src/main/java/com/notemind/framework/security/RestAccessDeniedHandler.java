package com.notemind.framework.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 已认证但无权限访问时的响应（403）。
 *
 * <p>与 401 区分开：401 表示"你是谁不知道"，403 表示"知道你是谁，但不允许"。
 * 前端对两者的处理不同（401 跳登录，403 只提示）。
 */
@Slf4j
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.debug("无权限访问: {} {}", request.getMethod(), request.getRequestURI());
        SecurityResponseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, 403,
                "没有访问权限");
    }
}
