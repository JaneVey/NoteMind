package com.notemind.auth.service;

import com.notemind.auth.dto.LoginRequest;
import com.notemind.auth.dto.LoginResponse;
import com.notemind.auth.dto.RegisterRequest;

/**
 * 认证服务。
 *
 * <p>按《后端开发规范》1.4，Service 暴露接口、实现类以 {@code Impl} 结尾。
 */
public interface AuthService {

    /** 注册新用户 */
    void register(RegisterRequest request);

    /** 登录并签发 Token */
    LoginResponse login(LoginRequest request);
}
