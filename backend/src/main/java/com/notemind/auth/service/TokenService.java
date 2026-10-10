package com.notemind.auth.service;

import com.notemind.auth.token.IssuedTokens;
import com.notemind.auth.token.RefreshSession;

import java.time.Duration;

/**
 * 凭据签发服务：负责 Access Token 与 Refresh Token 的完整生命周期。
 *
 * <p><b>职责边界</b>：本服务只做"凭据机制"，不碰 HTTP
 * （不接收 {@code HttpServletRequest}、不写 Cookie）。
 * Cookie 的读写在 Controller 层完成 —— 这是《后端开发规范》要求的
 * "Service 层禁止出现 Web 概念"，也让本服务可以脱离 Web 上下文单测。
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
     * 校验并<b>消费</b>一个 refresh token，返回其会话上下文。
     *
     * <p>这个方法会<b>作废旧 token</b>（标记为已用并删除），实现<b>轮换</b>。
     * 调用方拿到 {@link RefreshSession} 后应再调 {@link #issue} 签发新的一对凭据。
     *
     * <p><b>重用检测</b>：如果传入的 token 曾经被轮换过（说明可能存在凭据盗用），
     * 会吊销该用户<b>全部</b>会话并抛出异常，强制其重新登录。
     *
     * @throws com.notemind.common.exception.BusinessException token 无效、过期或检测到重用
     */
    RefreshSession consume(String refreshToken);

    /** 吊销单个 refresh token（登出） */
    void revoke(String refreshToken);

    /** 计算 refresh token 有效期 */
    Duration refreshTtl(boolean rememberMe);
}
