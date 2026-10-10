package com.notemind.auth.service;

import com.notemind.auth.token.IssuedTokens;
import com.notemind.auth.token.RefreshSession;

import java.time.Duration;

/**
 * 凭据签发服务：Access Token 与 Refresh Token 的生命周期。
 *
 * <p>不碰 HTTP（不接收 {@code HttpServletRequest}、不写 Cookie），Cookie 的读写在 Controller。
 */
public interface TokenService {

    /**
     * 签发一对新凭据（登录成功后调用）。
     *
     * @param userId     用户 id
     * @param username   用户名（写入 access token 声明，使过滤器无需查库）
     * @param rememberMe 是否"记住我"，决定 refresh token 有效期
     * @param userAgent  客户端 UA，仅用于"登录设备"展示
     * @param clientIp   客户端 IP，仅用于"登录设备"展示
     */
    IssuedTokens issue(Long userId, String username, boolean rememberMe, String userAgent, String clientIp);

    /**
     * 校验并消费一个 refresh token，返回其会话上下文。
     *
     * <p>本方法会作废旧 token（标记为已用并删除），即轮换。
     * 调用方拿到 {@link RefreshSession} 后应再调 {@link #issue} 签发新的一对凭据。
     *
     * <p>若传入的 token 曾经被轮换过（可能存在凭据盗用），会吊销该用户全部会话再抛异常。
     *
     * @throws com.notemind.common.exception.BusinessException token 无效、过期或检测到重用
     */
    RefreshSession consume(String refreshToken);

    /** 吊销单个 refresh token（登出） */
    void revoke(String refreshToken);

    /** 计算 refresh token 有效期 */
    Duration refreshTtl(boolean rememberMe);
}
