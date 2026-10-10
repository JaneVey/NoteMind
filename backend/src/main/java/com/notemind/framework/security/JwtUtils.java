package com.notemind.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Access Token（JWT）的签发与校验。
 *
 * <p><b>变更记录（2026-10-08，认证重设计）</b>：
 * <ol>
 *   <li><b>从 {@code com.notemind.auth.util} 移到 {@code framework.security}</b> ——
 *       原先 {@link JwtAuthenticationFilter}（基础设施）要 import 认证模块的工具类，
 *       是「基础设施依赖业务模块」的反向依赖。JWT 签发是基础设施能力，不属于任何业务模块。</li>
 *   <li>新增 <b>{@code jti}</b>（唯一 ID）声明 —— 为将来做 access token 黑名单预留；
 *       也更利于日志追踪同一次会话的多条请求</li>
 *   <li>新增 <b>{@code typ=access}</b> 声明 —— 明确这个 JWT 的用途。
 *       将来若签发其它用途的 JWT（如邮箱验证链接），过滤器可据此拒绝，避免混用</li>
 *   <li>新增 <b>{@code kid}</b>（密钥 ID）头 —— 支持<b>无损密钥轮换</b>：
 *       服务端可同时持有新旧两把密钥，靠 kid 选择验签密钥，用户无需重新登录</li>
 *   <li>有效期由 24 小时缩短为 <b>30 分钟</b>（配置项）—— 这是双 Token 方案的核心：
 *       access 短命，即使泄露窗口也很小；长期凭据交给 Refresh Token（HttpOnly Cookie）</li>
 * </ol>
 *
 * <p><b>注意</b>：本类只负责 <b>Access Token</b>。
 * Refresh Token 是<b>不透明随机串而非 JWT</b>（JWT 无状态、天生无法吊销，
 * 而"可吊销"正是 Refresh Token 要解决的问题），由 {@code TokenService} 管理。
 */
@Slf4j
@Component
public class JwtUtils {

    /** 本 JWT 的用途标记，防止不同用途的 JWT 被混用 */
    public static final String TYPE_ACCESS = "access";

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TYPE = "typ";

    private final SecretKey key;
    private final long expiration;
    private final String keyId;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expiration,
                    @Value("${jwt.key-id:notemind-1}") String keyId) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.keyId = keyId;
    }

    /**
     * 签发 Access Token。
     *
     * <p>subject 存 userId，另带 username 声明，使过滤器**无需查库**即可构造 {@link LoginUser}。
     */
    public String generateAccessToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 校验签名并解析声明。Token 无效、过期或类型不符时抛出 {@link JwtException}。
     *
     * <p>过滤器使用本方法而非"先 validate 再 parse"，避免同一请求内重复解析两次。
     */
    public Claims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 只接受 access 类型：防止将来其它用途的 JWT 被拿来当访问凭据
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Token 类型不是 access");
        }
        return claims;
    }

    public Long parseUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /** Access Token 有效期（毫秒），供接口返回 {@code expiresIn} 使用 */
    public long getExpirationMillis() {
        return expiration;
    }
}
