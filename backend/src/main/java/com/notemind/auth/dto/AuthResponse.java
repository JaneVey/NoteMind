package com.notemind.auth.dto;

import com.notemind.user.vo.UserProfileVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证成功响应（登录 / 刷新 共用）。
 *
 * <p>响应体里没有 refresh token —— 它在 HttpOnly Cookie 里。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Access Token（JWT）。前端只放内存，不进 localStorage */
    private String accessToken;

    /** Access Token 剩余有效秒数，供前端预判刷新时机 */
    private long expiresIn;

    /** 当前用户信息（不含密码等敏感字段） */
    private UserProfileVO user;
}
