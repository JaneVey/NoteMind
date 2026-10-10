package com.notemind.user.vo;

import lombok.Data;

/**
 * 当前登录用户的对外信息。
 *
 * <p><b>注意</b>：本类是"当前登录者"的视图，与 {@code SysUser} 实体刻意区分开 ——
 * 实体含 {@code password}，**绝不能直接返回给前端**（见《后端开发规范》领域模型边界）。
 *
 * <p>字段名用 {@code userId} 而非 {@code id}，因为它在语义上是"我的 id"。
 *
 * <p><b>变更记录（2026-10-08）</b>：新增 {@code emailVerified}。
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
