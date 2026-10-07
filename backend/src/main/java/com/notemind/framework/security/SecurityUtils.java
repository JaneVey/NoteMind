package com.notemind.framework.security;

import com.notemind.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户的读取工具。
 *
 * <p>业务代码统一通过此类获取当前用户，不要在 Controller 里反复写
 * {@code (Long) authentication.getPrincipal()} —— 那种写法有两个问题：
 * 一是重复且容易在重构时漏改，二是强转失败时抛出的是难以定位的 ClassCastException。
 *
 * <p>设计原则（见技术栈选型"用户与业务解耦"）：
 * 业务模块只通过 {@code userId} 关联用户，不感知 JWT 与 Security 上下文细节。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户 ID。未登录时抛出 {@link UnauthorizedException}（由全局异常处理器转为 401）。
     */
    public static Long currentUserId() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 获取当前登录用户 ID，未登录时返回 {@code null}。
     * 适用于"登录与否都要处理"的场景（例如可选的个性化）。
     */
    public static Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getId();
        }
        return null;
    }

    /**
     * 获取当前登录用户的用户名。
     *
     * <p>注意：用户名来自 JWT 的 claim，不查数据库。若需要昵称、头像、邮箱等完整信息，
     * 请按 userId 查询 {@code note_user} 表。
     */
    public static String currentUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUsername();
        }
        return null;
    }
}
