package com.notemind.framework.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 从请求中提取客户端信息（IP、UA）。
 *
 * <p>用于登录审计与"登录设备"展示。
 *
 * <p><b>⚠️ 关于 {@code X-Forwarded-For}</b>：这个头是<b>可以伪造</b>的。
 * 只有在请求确实经过我们信任的反向代理（Nginx）时它才有意义；
 * 若服务直接暴露公网，攻击者可以随便填。
 *
 * <p>本项目部署时 Nginx 在应用前面，因此优先读该头；
 * 但**不要把它当作安全边界** —— 它只用于日志与展示，不用于权限判定。
 */
public final class RequestUtils {

    private static final String[] IP_HEADERS = {"X-Forwarded-For", "X-Real-IP"};

    private static final String UNKNOWN = "unknown";

    private RequestUtils() {
    }

    /** 取客户端 IP。可能返回代理链上的第一个地址 */
    public static String clientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !UNKNOWN.equalsIgnoreCase(value)) {
                // X-Forwarded-For 可能是 "客户端, 代理1, 代理2"，取第一个
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** 取客户端 UA，缺失时返回空串（避免调用方到处判空） */
    public static String userAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua == null ? "" : ua;
    }
}
