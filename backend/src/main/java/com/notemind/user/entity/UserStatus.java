package com.notemind.user.entity;

/**
 * 账号状态取值。
 *
 * <p>数据库里 {@code sys_user.status} 是 {@code VARCHAR(20)} 而非 PG 的 ENUM 类型 ——
 * 改枚举值要写 DDL 迁移，不值得。这里用常量持有取值，避免魔法字符串散落各处。
 */
public final class UserStatus {

    /** 正常可用 */
    public static final String ACTIVE = "active";

    /** 已禁用，禁止登录 */
    public static final String DISABLED = "disabled";

    private UserStatus() {
    }
}
