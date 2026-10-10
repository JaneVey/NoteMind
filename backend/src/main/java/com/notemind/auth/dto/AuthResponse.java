package com.notemind.auth.dto;

import com.notemind.user.vo.UserProfileVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证成功响应（登录 / 刷新 共用）。
 *
 * <p><b>变更记录（2026-10-08）</b>：由 {@code LoginResponse} 改名并重构。
 * 原实现把 token 与用户字段平铺在一起、且字段有兼容分支（{@code user?: Partial<UserInfo>}），
 * 导致前端 store 里写满 {@code data.userId ?? data.user?.userId ?? null} 之类的兜底。
 *
 * <p><b>关键设计</b>：响应体里**只有 access token，没有 refresh token** ——
 * refresh token 通过 <b>HttpOnly Cookie</b> 下发，JavaScript 读不到，
 * 这样即使发生 XSS，攻击者也拿不到长期凭据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Access Token（JWT）。前端<b>只放内存</b>，不进 localStorage */
    private String accessToken;

    /** Access Token 剩余有效秒数，供前端预判刷新时机 */
    private long expiresIn;

    /** 当前用户信息（不含密码等敏感字段） */
    private UserProfileVO user;
}
