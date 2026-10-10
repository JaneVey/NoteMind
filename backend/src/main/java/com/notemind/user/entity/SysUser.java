package com.notemind.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 用户（数据库表 {@code sys_user}）。
 *
 * <p><b>变更记录（2026-10-08，认证重设计）</b>：
 * <ul>
 *   <li>{@code username} / {@code email} 在数据库里是 <b>CITEXT</b>（大小写不敏感唯一），
 *       Java 侧仍是 {@code String}，无需特殊处理 —— 只要不在应用层做
 *       {@code equalsIgnoreCase} 之类的补偿（那是多余的，且会掩盖问题）</li>
 *   <li>{@code password} <b>可为 null</b>：通过 GitHub 等第三方注册的账号没有密码。
 *       登录时若为 null，必须明确提示"该账号使用第三方登录"，而不是当作密码错误</li>
 *   <li>{@code email} 必填且唯一 —— 它是账号找回的唯一途径</li>
 *   <li>新增 {@code emailVerified} / {@code status} / {@code lastLoginAt}</li>
 * </ul>
 *
 * <p><b>注意</b>：本类含 {@code password} 字段，**绝不可直接返回给前端**
 * （见《后端开发规范》领域模型边界）。对外一律转 {@code UserProfileVO}。
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

    /** BCrypt 哈希。<b>为 null 表示该账号只能通过第三方登录</b> */
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
