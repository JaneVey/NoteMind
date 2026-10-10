package com.notemind.auth.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Refresh Token 的 Redis 存储。
 *
 * <p><b>为什么放 Redis 而不是建表</b>：TTL 天然契合"到期即失效"，
 * 而"查看登录设备"通过 {@code refresh:user:{userId}} 这个集合实现，无需扫全库。
 *
 * <h3>键设计</h3>
 * <pre>
 *   refresh:{sha256}       → HASH { userId, rememberMe, userAgent, clientIp }  TTL 7 天 / 30 天
 *   refresh:user:{userId}  → SET(该用户的全部 tokenHash)                        用于列出设备与强制下线
 *   refresh:used:{sha256}  → userId                                            TTL = 原剩余有效期
 * </pre>
 *
 * <h3>两个关键安全设计</h3>
 * <ol>
 *   <li><b>本类接收明文 token，内部做 SHA-256 后再存</b> ——
 *       调用方无法误把原文写进 Redis。与密码同理：Redis 被读走也不能直接使用。</li>
 *   <li><b>{@code refresh:used:} 里存的是 userId 而不是 "1"</b> ——
 *       这样检测到 token 重用时，能立刻知道该吊销<b>谁</b>的全部会话。
 *       如果只存标记，旧 token 已从 {@code refresh:} 删除，就找不到归属了。</li>
 * </ol>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_REFRESH = "refresh:";
    private static final String KEY_USER = "refresh:user:";
    private static final String KEY_USED = "refresh:used:";

    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_USER_AGENT = "userAgent";
    private static final String FIELD_CLIENT_IP = "clientIp";
    private static final String FIELD_REMEMBER_ME = "rememberMe";

    private final StringRedisTemplate redis;

    /** 保存一条会话记录（明文 token 传入，内部哈希） */
    public void save(String rawToken, Long userId, boolean rememberMe,
                     String userAgent, String clientIp, Duration ttl) {
        String hash = sha256(rawToken);
        String key = KEY_REFRESH + hash;

        redis.opsForHash().put(key, FIELD_USER_ID, userId.toString());
        redis.opsForHash().put(key, FIELD_REMEMBER_ME, Boolean.toString(rememberMe));
        if (userAgent != null) {
            redis.opsForHash().put(key, FIELD_USER_AGENT, truncate(userAgent, 400));
        }
        if (clientIp != null) {
            redis.opsForHash().put(key, FIELD_CLIENT_IP, truncate(clientIp, 60));
        }
        redis.expire(key, ttl);

        // 用户维度的索引，用于"列出登录设备"与"全部下线"
        String userKey = KEY_USER + userId;
        redis.opsForSet().add(userKey, hash);
        // 索引的 TTL 取最长值，保证它不会比其成员先过期
        redis.expire(userKey, ttl);
    }

    /**
     * 按明文 token 查会话上下文。
     *
     * @return token 不存在（已过期 / 已轮换 / 伪造）时返回 {@link Optional#empty()}
     */
    public Optional<RefreshSession> find(String rawToken) {
        Map<Object, Object> entries = redis.opsForHash().entries(KEY_REFRESH + sha256(rawToken));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        try {
            Long userId = Long.valueOf(entries.get(FIELD_USER_ID).toString());
            boolean rememberMe = Boolean.parseBoolean(
                    String.valueOf(entries.getOrDefault(FIELD_REMEMBER_ME, "false")));
            return Optional.of(new RefreshSession(userId, rememberMe));
        } catch (RuntimeException e) {
            log.warn("Redis 中的 refresh 记录格式非法，已忽略: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 查询该 token 是否<b>曾经被轮换过</b>（即被使用过）。
     *
     * @return 命中返回原持有者的 userId（说明发生了重用，调用方应吊销其全部会话）；否则返回 null
     */
    public Long findReusedTokenOwner(String rawToken) {
        String value = redis.opsForValue().get(KEY_USED + sha256(rawToken));
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 把 token 标记为"已使用"，TTL 取其原本的剩余有效期。
     *
     * <p>轮换时调用：旧 token 从 {@code refresh:} 删除，但在 {@code refresh:used:} 留痕，
     * 以便它被再次使用时能识别出重用。
     */
    public void markUsed(String rawToken, Long userId) {
        String hash = sha256(rawToken);
        String refreshKey = KEY_REFRESH + hash;

        Duration remaining = Duration.ofDays(1);
        Long expireSeconds = redis.getExpire(refreshKey);
        if (expireSeconds != null && expireSeconds > 0) {
            remaining = Duration.ofSeconds(expireSeconds);
        }
        redis.opsForValue().set(KEY_USED + hash, userId.toString(), remaining);
    }

    /** 吊销单个 token（登出） */
    public void remove(String rawToken, Long userId) {
        String hash = sha256(rawToken);
        redis.delete(KEY_REFRESH + hash);
        if (userId != null) {
            redis.opsForSet().remove(KEY_USER + userId, hash);
        }
    }

    /** 吊销某用户的全部 token（重用检测触发，或"强制全部下线"） */
    public void removeAll(Long userId) {
        Set<String> hashes = listTokenHashes(userId);
        for (String hash : hashes) {
            redis.delete(KEY_REFRESH + hash);
        }
        redis.delete(KEY_USER + userId);

        if (!hashes.isEmpty()) {
            log.warn("已吊销用户的全部会话: userId={} 数量={}", userId, hashes.size());
        }
    }

    /** 列出某用户当前的 tokenHash（"登录设备"功能的雏形） */
    public Set<String> listTokenHashes(Long userId) {
        Set<String> members = redis.opsForSet().members(KEY_USER + userId);
        return members == null ? Collections.emptySet() : members;
    }

    /** SHA-256 十六进制摘要 */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // JDK 必然支持 SHA-256，走到这里说明运行环境异常
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
