package com.notemind.auth.dto;

import com.notemind.common.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求。
 *
 * <p>email 必填：它是账号找回的唯一途径。
 *
 * <p>数据库里 username 与 email 都是 CITEXT（大小写不敏感唯一），
 * 所以应用层不需要再做 lower() 处理。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度 3-50 个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和连字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = PasswordPolicy.LENGTH_MESSAGE)
    @Pattern(regexp = PasswordPolicy.ALLOWED_REGEX, message = PasswordPolicy.CHARSET_MESSAGE)
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String email;

    @Size(max = 100, message = "昵称不能超过 100 个字符")
    private String nickname;
}
