package com.notemind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求。
 *
 * <p>identifier 刻意不做格式校验：它可能是用户名也可能是邮箱，加正则只会误伤。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "请输入用户名或邮箱")
    private String identifier;

    @NotBlank(message = "请输入密码")
    private String password;

    /** 记住我。null 视为 false */
    private Boolean rememberMe;
}
