package com.notemind.framework.security;

import com.notemind.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户的读取工具。业务代码统一从这里取当前用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户 ID。未登录时抛出 {@link UnauthorizedException}（由全局异常处理器转为 401） */
    public static Long currentUserId() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return userId;
    }

    /** 获取当前登录用户 ID，未登录时返回 {@code null} */
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

    /** 获取当前登录用户的用户名。来自 JWT 声明，不查库；昵称头像等需另查 */
    public static String currentUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUsername();
        }
        return null;
    }
}
