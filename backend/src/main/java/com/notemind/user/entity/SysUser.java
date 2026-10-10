package com.notemind.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 用户（数据库表 {@code sys_user}）。
 *
 * <p>两个非直觉点：{@code username} / {@code email} 在库里是 CITEXT（大小写不敏感唯一），
 * 应用层不要做 {@code equalsIgnoreCase} 之类的补偿；{@code password} 可为 null，
 * 表示这是第三方登录注册的账号，登录时必须提示改用第三方登录而不是报密码错误。
 *
 * <p>本类含 {@code password}，绝不可直接返回给前端，对外一律转 {@code UserProfileVO}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名。数据库为 citext，大小写不敏感唯一 */
    private String username;

    /** 邮箱。必填、唯一，大小写不敏感 */
    private String email;

    /** 邮箱是否已验证。未验证的账号仍可正常使用，界面引导验证 */
    private Boolean emailVerified;

    /** BCrypt 哈希。为 null 表示该账号只能通过第三方登录 */
    private String password;

    private String nickname;

    private String avatar;

    /** 账号状态：active / disabled。用字符串而非 PG ENUM，避免改值要写 DDL */
    private String status;

    /** 最近登录时间（登录审计） */
    private OffsetDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
