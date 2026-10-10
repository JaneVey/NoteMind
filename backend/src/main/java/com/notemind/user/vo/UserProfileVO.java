package com.notemind.user.vo;

import lombok.Data;

/**
 * 当前登录用户的对外信息。
 *
 * <p>与 {@code SysUser} 实体刻意分开：实体含 {@code password}，不能直接返回给前端。
 * 字段名用 {@code userId} 而非 {@code id}，因为语义上是"我的 id"。
 */
@Data
public class UserProfileVO {

    private Long userId;

    private String username;

    private String email;

    /** 邮箱是否已验证。未验证的账号可正常使用，界面引导验证 */
    private Boolean emailVerified;

    private String nickname;

    private String avatar;
}
