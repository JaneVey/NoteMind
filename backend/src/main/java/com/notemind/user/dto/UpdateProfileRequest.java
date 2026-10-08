package com.notemind.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求。
 *
 * <p>此前接口用 {@code Map<String, String>} 接收 —— 违反《后端开发规范》5.4：
 * Map 无法做参数校验、无法生成接口文档、字段拼写错误在编译期无法发现。
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "昵称不能超过 100 个字符")
    private String nickname;

    @Size(max = 500, message = "头像地址不能超过 500 个字符")
    private String avatar;
}
