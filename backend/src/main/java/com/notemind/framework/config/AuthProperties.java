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

    private LoginThrottle loginThrottle = new LoginThrottle();

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

    /** 登录防爆破。按 IP 限流为主，账号维度只做递进延迟，不锁定账号 */
    @Data
    public static class LoginThrottle {

        /** 失败计数的统计窗口（分钟） */
        private int windowMinutes = 15;

        /** 同一 IP 在窗口内允许的最大失败次数，达到即拒绝 */
        private int ipMaxFailures = 20;

        /** 同一账号失败达到该次数后开始递进延迟 */
        private int accountDelayThreshold = 5;

        /** 递进延迟的基数（毫秒） */
        private long baseDelayMillis = 500;

        /** 递进延迟上限（毫秒） */
        private long maxDelayMillis = 2000;
    }
}
