package com.notemind.auth.service;

import com.notemind.auth.dto.AuthResult;
import com.notemind.auth.dto.LoginRequest;
import com.notemind.auth.dto.RegisterRequest;

/**
 * 认证服务。
 *
 * <p>不接触 HTTP：Cookie 的读写在 Controller 完成，本服务只通过参数接收 / 返回 token 字符串。
 */
public interface AuthService {

    /** 注册新用户。注册成功后不自动登录，由前端引导登录 */
    void register(RegisterRequest request);

    /**
     * 登录，支持用户名或邮箱。
     *
     * @param userAgent 客户端 UA（记入会话，用于"登录设备"展示）
     * @param clientIp  客户端 IP
     * @return 响应体内容 + 待写入 Cookie 的 refresh token
     */
    AuthResult login(LoginRequest request, String userAgent, String clientIp);

    /**
     * 用 refresh token 换取新的一对凭据。
     *
     * <p>内部完成轮换：旧 refresh token 会作废。
     *
     * @return 新的响应体内容 + 新的 refresh token
     */
    AuthResult refresh(String refreshToken, String userAgent, String clientIp);

    /** 登出：吊销该 refresh token。token 为空时静默返回（幂等） */
    void logout(String refreshToken);
}
