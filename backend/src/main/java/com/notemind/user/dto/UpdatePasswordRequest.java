package com.notemind.user.dto;

import com.notemind.common.validation.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

    /** 原密码只校验非空 —— 老密码可能不满足当前规则，不能按新规则拦 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = PasswordPolicy.LENGTH_MESSAGE)
    @Pattern(regexp = PasswordPolicy.ALLOWED_REGEX, message = PasswordPolicy.CHARSET_MESSAGE)
    private String newPassword;
}
