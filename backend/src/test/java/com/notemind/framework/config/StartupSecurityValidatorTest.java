package com.notemind.framework.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 启动强校验的判定规则。
 *
 * <p>{@code findViolations} 与 Spring 解耦，这里直接喂配置值，不需要起应用上下文。
 */
class StartupSecurityValidatorTest {

    private static final String STRONG_SECRET = "0123456789abcdef0123456789abcdef01234567";
    private static final List<String> GOOD_ORIGINS = List.of("https://notemind.example.com");

    private static List<String> violationsOf(String secret, boolean cookieSecure, List<String> origins) {
        return StartupSecurityValidator.findViolations(secret, cookieSecure, origins);
    }

    @Test
    @DisplayName("全部合规时返回空列表")
    void allGoodPasses() {
        assertThat(violationsOf(STRONG_SECRET, true, GOOD_ORIGINS)).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("仓库里出现过的占位密钥一律拒绝，哪怕长度够")
    @ValueSource(strings = {
            "notemind-LOCAL-DEV-ONLY-secret-change-me-in-production-must-be-long-enough",
            "change-me-to-a-random-string-at-least-32-bytes-long",
    })
    void rejectsPlaceholderSecrets(String placeholder) {
        assertThat(placeholder.length()).as("占位值本身长于 32 字节，所以要靠按值比对拦截")
                .isGreaterThanOrEqualTo(StartupSecurityValidator.MIN_SECRET_BYTES);
        assertThat(violationsOf(placeholder, true, GOOD_ORIGINS))
                .anySatisfy(v -> assertThat(v).contains("占位值"));
    }

    @Test
    @DisplayName("密钥不足 32 字节时拒绝 —— HS256 需要 256 位")
    void rejectsShortSecret() {
        assertThat(violationsOf("a".repeat(31), true, GOOD_ORIGINS))
                .anySatisfy(v -> assertThat(v).contains("32 字节"));
    }

    @Test
    @DisplayName("刚好 32 字节的随机密钥通过")
    void acceptsExactly32Bytes() {
        assertThat(violationsOf("a".repeat(32), true, GOOD_ORIGINS)).isEmpty();
    }

    @Test
    @DisplayName("cookie-secure 为 false 时拒绝 —— 长期凭据会走明文 HTTP")
    void rejectsInsecureCookie() {
        assertThat(violationsOf(STRONG_SECRET, false, GOOD_ORIGINS))
                .anySatisfy(v -> assertThat(v).contains("cookie-secure"));
    }

    @ParameterizedTest
    @DisplayName("CORS 含 * 时拒绝，带空格的写法也要认出来")
    @ValueSource(strings = {"*", " * ", "https://a.example.com,*"})
    void rejectsWildcardCors(String origin) {
        String[] parts = origin.split(",");
        assertThat(violationsOf(STRONG_SECRET, true, List.of(parts)))
                .anySatisfy(v -> assertThat(v).contains("allowed-origins"));
    }

    @Test
    @DisplayName("多项不合规时一次全部列出，而不是只报第一条")
    void reportsAllViolations() {
        List<String> violations = violationsOf("short", false, List.of("*"));
        assertThat(violations).hasSize(3);
    }

    @Test
    @DisplayName("来源列表为空或为 null 不算违规（配置缺失由别处兜底）")
    void nullOriginsTolerated() {
        assertThat(violationsOf(STRONG_SECRET, true, null)).isEmpty();
        assertThat(violationsOf(STRONG_SECRET, true, List.of())).isEmpty();
    }
}
