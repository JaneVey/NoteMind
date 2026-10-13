package com.notemind.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录失败计数的键与窗口规则。用 mock 的 Redis 模板，不需要真的起 Redis。
 */
class LoginAttemptStoreTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private LoginAttemptStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        store = new LoginAttemptStore(redis);
    }

    @Test
    @DisplayName("账号标识统一小写 —— 否则变换大小写就能把失败次数摊到多个键上绕开延迟")
    void normalizesIdentifierCase() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        store.recordIdentifierFailure("Admin@Example.COM", WINDOW);

        verify(valueOps).increment("auth:fail:id:admin@example.com");
    }

    @Test
    @DisplayName("只有首个失败设过期，后续沿用同一窗口 —— 否则持续攻击会让窗口无限续期")
    void setsExpiryOnlyOnFirstFailure() {
        when(valueOps.increment(anyString())).thenReturn(1L, 2L);

        store.recordIdentifierFailure("admin", WINDOW);
        store.recordIdentifierFailure("admin", WINDOW);

        verify(redis, times(1)).expire("auth:fail:id:admin", WINDOW);
    }

    @Test
    @DisplayName("成功登录只清账号计数，不动 IP 计数 —— IP 计数衡量的是来源地址")
    void clearsOnlyAccountCounter() {
        store.clearIdentifierFailures("Admin");

        verify(redis).delete("auth:fail:id:admin");
        verify(redis, never()).delete(startsWith("auth:fail:ip:"));
    }

    @Test
    @DisplayName("IP 维度用独立前缀，不与账号维度混用")
    void ipUsesSeparatePrefix() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        store.recordIpFailure("203.0.113.7", WINDOW);

        verify(valueOps).increment("auth:fail:ip:203.0.113.7");
    }

    @Test
    @DisplayName("计数键里的值非法时按 0 处理，不抛异常")
    void malformedCounterTreatedAsZero() {
        when(valueOps.get("auth:fail:id:admin")).thenReturn("not-a-number");

        assertThat(store.countIdentifierFailures("admin")).isZero();
    }

    @Test
    @DisplayName("键不存在时计数为 0")
    void missingKeyCountsAsZero() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(store.countIdentifierFailures("nobody")).isZero();
        assertThat(store.countIpFailures("203.0.113.7")).isZero();
    }
}
