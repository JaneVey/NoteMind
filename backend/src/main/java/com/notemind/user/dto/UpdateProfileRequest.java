package com.notemind.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求。
 *
 * <p>入参用带校验注解的 DTO，不用 {@code Map<String, String>} ——
 * Map 无法做参数校验，字段拼错到编译期也发现不了。
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "昵称不能超过 100 个字符")
    private String nickname;

    @Size(max = 500, message = "头像地址不能超过 500 个字符")
    private String avatar;
}
