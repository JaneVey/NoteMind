package com.notemind.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证相关配置（{@code app.auth.*}）。
 *
 * <p>用 {@code @ConfigurationProperties} 而不是散落的 {@code @Value}：
 * 相关配置集中在一处、有类型、有默认值，改的时候不会漏。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private RefreshToken refreshToken = new RefreshToken();

    @Data
    public static class RefreshToken {

        /** Cookie 名 */
        private String cookieName = "notemind_rt";

        /**
         * Cookie 作用路径。
         *
         * <p>限定为 {@code /api/auth} 而不是 {@code /} —— 浏览器只在访问这些路径时才带上它，
         * 减少暴露面（其它接口根本不会携带 refresh token）。
         */
        private String cookiePath = "/api/auth";

        /**
         * 是否只在 HTTPS 下发送（{@code Secure} 属性）。
         *
         * <p>⚠️ <b>生产必须为 true</b>。开发环境走 http://localhost，Chrome 虽把 localhost
         * 视为安全上下文、允许 Secure Cookie，但为避免不同浏览器行为差异，开发期默认关闭。
         */
        private boolean cookieSecure = false;

        /** SameSite 策略。Lax 已能挡住跨站 POST，是本方案 CSRF 防护的依据 */
        private String cookieSameSite = "Lax";

        /** 默认有效期（天） */
        private long ttlDays = 7;

        /** 勾选"记住我"后的有效期（天） */
        private long rememberMeTtlDays = 30;
    }
}
