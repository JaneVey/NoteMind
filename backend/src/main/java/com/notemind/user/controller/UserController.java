package com.notemind.user.controller;

import com.notemind.common.result.Result;
import com.notemind.framework.security.SecurityUtils;
import com.notemind.user.dto.UpdatePasswordRequest;
import com.notemind.user.dto.UpdateProfileRequest;
import com.notemind.user.service.UserService;
import com.notemind.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户中心接口。
 *
 * <p><b>变更记录</b>
 * <ul>
 *   <li>2026-10-07：改用 {@link SecurityUtils#currentUserId()}，不再手写
 *       {@code (Long) authentication.getPrincipal()} 强转；</li>
 *   <li>2026-10-08：注入 {@link UserService} 而非 {@code SysUserMapper} ——
 *       Controller 不再承载业务逻辑（此前它直接查库、判断密码、拼 VO）；
 *       入参由 {@code Map<String, String>} 改为带校验注解的 DTO。</li>
 * </ul>
 *
 * <p>本类的职责被限定为三件事：<b>取当前用户 → 调 Service → 包装响应</b>。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        return Result.success(userService.getProfile(SecurityUtils.currentUserId()));
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(SecurityUtils.currentUserId(), request);
        return Result.success();
    }
}
