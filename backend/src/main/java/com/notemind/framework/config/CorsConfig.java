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
 * <p>必须用显式域名白名单，不能用 {@code allowedOriginPatterns("*")} ——
 * 它回显任何来源，叠加 {@code allowCredentials(true)} 后任意站点都能带用户凭据跨站请求。
 * 允许来源读 {@code app.cors.allowed-origins}，生产环境由 {@code APP_CORS_ALLOWED_ORIGINS} 注入。
 *
 * <p>由 {@link com.notemind.framework.security.SecurityConfig} 通过 {@code http.cors()} 启用。
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
