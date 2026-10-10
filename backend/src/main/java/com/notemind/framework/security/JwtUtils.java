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
 * <p>本类只管短命的 Access Token；Refresh Token 是不透明随机串，由 {@code TokenService} 管理。
 * JWT 带 {@code typ=access} 声明，将来若有其它用途的 JWT 可据此拒绝，避免混用。
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
     * <p>subject 存 userId，另带 username 声明，使过滤器无需查库即可构造 {@link LoginUser}。
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
