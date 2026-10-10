package com.notemind.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求。
 *
 * <p><b>变更记录（2026-10-08）</b>：{@code email} 此前**没有任何校验** ——
 * 而数据库已将它改为 {@code NOT NULL}，不填邮箱会直接撞约束、返回 500。
 * 现补上 {@code @NotBlank} + {@code @Email} + 长度上限。
 *
 * <p>为什么邮箱必填：它是**账号找回的唯一途径**。没有邮箱的账号一旦忘记密码就只能作废。
 *
 * <p>注意：数据库里 {@code username} 与 {@code email} 都是 <b>CITEXT</b>（大小写不敏感唯一），
 * 所以 {@code Admin} 与 {@code admin} 会被正确判定为重复 —— 应用层不需要再做 lower() 处理。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度 3-50 个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和连字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度 8-100 个字符")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String email;

    @Size(max = 100, message = "昵称不能超过 100 个字符")
    private String nickname;
}
