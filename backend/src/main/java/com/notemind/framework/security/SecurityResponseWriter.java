package com.notemind.framework.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * 认证 / 授权失败时的统一 JSON 响应写出工具。
 *
 * <p>手写 JSON、不注入序列化器：这段逻辑在 Spring Security 过滤器链里跑，早于 MVC 异常处理；
 * 而且 Spring Boot 4 用 Jackson 3，类路径上仍可能由 jjwt-jackson 间接引入 Jackson 2，混用会出问题。
 *
 * <p>响应体与业务接口的 {@code Result} 同构（{@code {code, message, data}}）。
 * message 均为内部固定文案，不含引号与反斜杠，因此不做转义。
 */
final class SecurityResponseWriter {

    private SecurityResponseWriter() {
    }

    static void write(HttpServletResponse response, int httpStatus, int code, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String body = "{\"code\":" + code
                + ",\"message\":\"" + message
                + "\",\"data\":null}";

        response.getWriter().write(body);
    }
}
