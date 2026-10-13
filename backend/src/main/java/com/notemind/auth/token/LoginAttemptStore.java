package com.notemind.auth.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Locale;

/**
 * 登录失败计数的 Redis 存储，供防爆破使用。
 *
 * <p>固定窗口：只在计数从 0 变 1 时设过期，之后的失败沿用同一个窗口 ——
 * 否则持续攻击会让窗口无限续期，计数永远不归零。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LoginAttemptStore {

    private static final String KEY_IP = "auth:fail:ip:";
    private static final String KEY_IDENTIFIER = "auth:fail:id:";

    private final StringRedisTemplate redis;

    /** 记一次失败，返回该 IP 在当前窗口内的失败次数 */
    public long recordIpFailure(String ip, Duration window) {
        return increment(KEY_IP + ip, window);
    }

    /** 记一次失败，返回该账号在当前窗口内的失败次数 */
    public long recordIdentifierFailure(String identifier, Duration window) {
        return increment(KEY_IDENTIFIER + normalize(identifier), window);
    }

    public long countIpFailures(String ip) {
        return read(KEY_IP + ip);
    }

    public long countIdentifierFailures(String identifier) {
        return read(KEY_IDENTIFIER + normalize(identifier));
    }

    /**
     * 登录成功，清掉该账号的失败计数。
     *
     * <p>不清 IP 计数：它衡量的是来源地址的可疑程度，与"这一次是谁登录成功"无关。
     */
    public void clearIdentifierFailures(String identifier) {
        redis.delete(KEY_IDENTIFIER + normalize(identifier));
    }

    private long increment(String key, Duration window) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }
        return count == null ? 0L : count;
    }

    private long read(String key) {
        String value = redis.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("登录失败计数的值非法，按 0 处理: key={}", key);
            return 0L;
        }
    }

    /**
     * 账号标识统一小写后再做键。
     *
     * <p>username 与 email 在库里是 CITEXT（大小写不敏感），若计数键区分大小写，
     * 攻击者只要变换大小写就能把失败次数摊到多个键上，绕过延迟。
     */
    private String normalize(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
    }
}
