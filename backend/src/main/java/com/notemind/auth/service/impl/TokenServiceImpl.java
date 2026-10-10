package com.notemind.auth.service.impl;

import com.notemind.auth.service.TokenService;
import com.notemind.auth.token.IssuedTokens;
import com.notemind.auth.token.RefreshSession;
import com.notemind.auth.token.RefreshTokenStore;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.framework.config.AuthProperties;
import com.notemind.framework.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * 凭据签发服务实现。
 *
 * <p>Access Token 是 JWT，服务端只验签不查库；Refresh Token 是 32 字节不透明随机串
 * （不是 JWT），必须在 Redis 里查得到才有效 —— 这正是它"可吊销"的原因。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    /** 32 字节 = 256 位熵，足以抵抗暴力猜测 */
    private static final int REFRESH_TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtUtils jwtUtils;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthProperties authProperties;

    @Override
    public IssuedTokens issue(Long userId, String username, boolean rememberMe,
                              String userAgent, String clientIp) {
        String accessToken = jwtUtils.generateAccessToken(userId, username);
        String refreshToken = generateOpaqueToken();
        Duration ttl = refreshTtl(rememberMe);

        refreshTokenStore.save(refreshToken, userId, rememberMe, userAgent, clientIp, ttl);

        return new IssuedTokens(
                accessToken,
                jwtUtils.getExpirationMillis() / 1000,
                refreshToken,
                ttl);
    }

    @Override
    public RefreshSession consume(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // ① 重用检测：这个 token 曾经被轮换过，却又被拿来用
        Long reusedOwner = refreshTokenStore.findReusedTokenOwner(refreshToken);
        if (reusedOwner != null) {
            log.warn("检测到 Refresh Token 重用，判定为凭据盗用，吊销该用户全部会话: userId={}", reusedOwner);
            refreshTokenStore.removeAll(reusedOwner);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // ② 正常查找（过期 / 已吊销 / 伪造 都会落到这里）
        RefreshSession session = refreshTokenStore.find(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        // ③ 轮换：旧 token 留痕后作废
        refreshTokenStore.markUsed(refreshToken, session.userId());
        refreshTokenStore.remove(refreshToken, session.userId());

        return session;
    }

    @Override
    public void revoke(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        refreshTokenStore.find(refreshToken).ifPresent(session ->
                refreshTokenStore.remove(refreshToken, session.userId()));
    }

    @Override
    public Duration refreshTtl(boolean rememberMe) {
        AuthProperties.RefreshToken cfg = authProperties.getRefreshToken();
        return Duration.ofDays(rememberMe ? cfg.getRememberMeTtlDays() : cfg.getTtlDays());
    }

    /**
     * 生成不透明随机串。
     * 用 {@link SecureRandom} 而非 {@code Math.random()} —— 后者可预测，不能用于凭据。
     */
    private String generateOpaqueToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
