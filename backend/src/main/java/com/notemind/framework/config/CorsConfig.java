package com.notemind.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 配置。
 *
 * <p><b>变更记录（2026-10-07）——这是一处真实的安全漏洞修复</b>
 *
 * <p>原配置为：
 * <pre>
 *   config.setAllowedOriginPatterns(List.of("*"));
 *   config.setAllowCredentials(true);
 * </pre>
 *
 * <p>这两行组合是典型的 CORS 配置错误：{@code allowedOriginPatterns("*")} 会<b>回显任何来源</b>，
 * 叠加 {@code allowCredentials(true)} 后，任意第三方站点都能携带用户凭据发起跨站请求。
 * （Spring 之所以提供 {@code allowedOriginPatterns} 而 {@code allowedOrigins("*")} 会直接报错，
 * 就是为了防止这种误用被无意识地写出来。）
 *
 * <p>现改为<b>显式域名白名单</b>，来源列表从 {@code app.cors.allowed-origins} 读取，
 * 默认只允许本地前端开发端口；生产环境通过环境变量 {@code APP_CORS_ALLOWED_ORIGINS} 注入真实域名。
 *
 * <p>说明：使用具体来源而非通配符时，{@code allowCredentials(true)} 是安全的。
 * 本 Bean 由 {@link com.notemind.framework.security.SecurityConfig} 通过 {@code http.cors()} 启用。
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (origins.isEmpty()) {
            log.warn("app.cors.allowed-origins 为空，跨域请求将全部被拒绝");
        } else {
            log.info("CORS 允许来源: {}", origins);
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // 让前端能读到下载 / SSE 相关的响应头
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
