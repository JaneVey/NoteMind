package com.notemind.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证相关配置（{@code app.auth.*}）。
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

        /** Cookie 作用路径。限定为 {@code /api/auth}，其它接口不会携带 refresh token */
        private String cookiePath = "/api/auth";

        /** 是否只在 HTTPS 下发送（Secure 属性）。生产必须为 true */
        private boolean cookieSecure = false;

        /** SameSite 策略。Lax 已能挡住跨站 POST，是本方案 CSRF 防护的依据 */
        private String cookieSameSite = "Lax";

        /** 默认有效期（天） */
        private long ttlDays = 7;

        /** 勾选"记住我"后的有效期（天） */
        private long rememberMeTtlDays = 30;
    }
}
