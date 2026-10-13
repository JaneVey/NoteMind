package com.notemind.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 启动前的安全强校验：关键配置仍是开发默认值时拒绝启动。
 *
 * <p>只在非 dev 环境生效。用 {@code !dev} 而不是 {@code prod}，是因为本项目没有
 * prod profile、容器部署也不设 profile —— 语义是「只要不是明确的开发环境就按生产对待」。
 *
 * <p>把「注释里写一句生产必须改」换成「启动即失败」，是因为本项目曾把真实 API Key
 * 硬编码进配置并提交到公开仓库：历史擦不掉，只能作废密钥。靠自觉必然出事。
 */
@Slf4j
@Component
@Profile("!dev")
public class StartupSecurityValidator implements ApplicationRunner {

    /** HS256 要求密钥至少 256 位 */
    static final int MIN_SECRET_BYTES = 32;

    /** 仓库里出现过的占位密钥。照着 .env.example 抄一份就部署，也能通过长度检查，因此按值拒绝 */
    static final Set<String> PLACEHOLDER_JWT_SECRETS = Set.of(
            "notemind-LOCAL-DEV-ONLY-secret-change-me-in-production-must-be-long-enough",
            "change-me-to-a-random-string-at-least-32-bytes-long");

    private final String jwtSecret;
    private final boolean cookieSecure;
    private final List<String> corsOrigins;
    private final Environment environment;

    public StartupSecurityValidator(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${app.auth.refresh-token.cookie-secure}") boolean cookieSecure,
            @Value("${app.cors.allowed-origins}") List<String> corsOrigins,
            Environment environment) {
        this.jwtSecret = jwtSecret;
        this.cookieSecure = cookieSecure;
        this.corsOrigins = corsOrigins;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> violations = findViolations(jwtSecret, cookieSecure, corsOrigins);
        if (violations.isEmpty()) {
            log.info("安全配置校验通过");
            return;
        }

        log.error("安全配置校验未通过，拒绝启动，共 {} 项：", violations.size());
        violations.forEach(v -> log.error("  - {}", v));
        log.error("本机开发可加 -Dspring-boot.run.profiles=dev 跳过校验（当前 profile: {}）",
                String.join(",", environment.getActiveProfiles()));

        throw new IllegalStateException("安全配置校验未通过：" + String.join("；", violations));
    }

    /**
     * 校验逻辑与 Spring 解耦，便于直接单测。
     *
     * @return 全部不合规项，空表示通过
     */
    static List<String> findViolations(String jwtSecret, boolean cookieSecure, List<String> corsOrigins) {
        List<String> violations = new ArrayList<>();

        if (PLACEHOLDER_JWT_SECRETS.contains(jwtSecret)) {
            violations.add("jwt.secret 仍是仓库里的占位值，必须换成随机密钥（openssl rand -base64 48）");
        } else if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            violations.add("jwt.secret 不足 " + MIN_SECRET_BYTES + " 字节，HS256 需要 256 位");
        }

        if (!cookieSecure) {
            violations.add("app.auth.refresh-token.cookie-secure 为 false，长期凭据会经明文 HTTP 发送");
        }

        if (corsOrigins != null && corsOrigins.stream().anyMatch(origin -> "*".equals(origin.trim()))) {
            violations.add("app.cors.allowed-origins 含 *，任意站点都能发起携带凭据的跨站请求");
        }

        return violations;
    }
}
