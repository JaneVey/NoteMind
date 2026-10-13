package com.notemind.framework.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 从请求中提取客户端信息（IP、UA），用于登录限流、登录审计与"登录设备"展示。
 *
 * <p>默认只取 TCP 对端地址。转发头（X-Forwarded-For / X-Real-IP）完全由客户端可控，
 * 拿它做限流或任何安全判断等于没做 —— 每次请求换一个值就能绕过。
 * 只有确实部署在可信反向代理之后时，才把 {@code app.security.trust-proxy-headers} 打开。
 */
@Component
public class RequestUtils {

    private static final String[] IP_HEADERS = {"X-Forwarded-For", "X-Real-IP"};

    private static final String UNKNOWN = "unknown";

    private final boolean trustProxyHeaders;

    public RequestUtils(@Value("${app.security.trust-proxy-headers:false}") boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
    }

    /** 取客户端 IP。默认返回不可伪造的对端地址 */
    public String clientIp(HttpServletRequest request) {
        if (trustProxyHeaders) {
            for (String header : IP_HEADERS) {
                String value = request.getHeader(header);
                if (StringUtils.hasText(value) && !UNKNOWN.equalsIgnoreCase(value)) {
                    // X-Forwarded-For 可能是 "客户端, 代理1, 代理2"，取第一个
                    int comma = value.indexOf(',');
                    return (comma > 0 ? value.substring(0, comma) : value).trim();
                }
            }
        }
        return request.getRemoteAddr();
    }

    /** 取客户端 UA，缺失时返回空串（避免调用方到处判空） */
    public String userAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua == null ? "" : ua;
    }
}
