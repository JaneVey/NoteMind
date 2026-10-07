package com.notemind.framework.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * 认证 / 授权失败时的统一 JSON 响应写出工具。
 *
 * <p><b>为什么手写 JSON 而不注入 ObjectMapper</b>：
 * <ol>
 *   <li>这段逻辑运行在 Spring Security 过滤器链中，早于 MVC 的异常处理，
 *       应尽量不依赖应用上下文中的序列化组件；</li>
 *   <li>Spring Boot 4 使用 <b>Jackson 3</b>（包名 {@code tools.jackson}），
 *       而类路径上仍可能残留 Jackson 2（由 jjwt-jackson 等间接引入）。
 *       在此处引入序列化器会造成两个 Jackson 版本混用。</li>
 * </ol>
 *
 * <p>响应体结构与业务接口的 {@code Result} 保持一致：{@code {code, message, data}}。
 * 由于 message 均为内部固定文案（不含引号与反斜杠），无需转义处理。
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
