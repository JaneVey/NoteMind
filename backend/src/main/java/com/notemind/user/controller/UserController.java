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
 * <p>职责限定为三件事：取当前用户 → 调 Service → 包装响应。
 * 当前用户一律走 {@link SecurityUtils#currentUserId()}，不要手写
 * {@code (Long) authentication.getPrincipal()} 强转。
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
