package com.notemind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求。
 *
 * <p><b>变更记录（2026-10-08）</b>：
 * <ul>
 *   <li>{@code username} 改名为 <b>{@code identifier}</b>，同时支持<b>用户名或邮箱</b>登录 ——
 *       市面常见做法（GitHub、Notion 都支持），减少用户"我当初用哪个注册的"的困惑</li>
 *   <li>新增 {@code rememberMe}：勾选后 Refresh Token 有效期由 7 天延长到 30 天</li>
 * </ul>
 *
 * <p><b>注意</b>：这里刻意**不做格式校验** —— identifier 可能是用户名也可能是邮箱，
 * 加正则只会误伤。真正的判定交给服务端按两种情况查询。
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
