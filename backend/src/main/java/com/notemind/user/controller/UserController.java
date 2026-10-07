package com.notemind.user.controller;

import com.notemind.common.exception.BusinessException;
import com.notemind.common.result.Result;
import com.notemind.entity.NoteUser;
import com.notemind.framework.security.SecurityUtils;
import com.notemind.mapper.NoteUserMapper;
import com.notemind.user.dto.UpdatePasswordRequest;
import com.notemind.user.dto.UserProfileDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户中心接口。
 *
 * <p><b>变更记录（2026-10-07）</b>：原先每个方法都写
 * {@code Long userId = (Long) authentication.getPrincipal();} 并各自做空判断。
 * 现统一改为 {@link SecurityUtils#currentUserId()}（从 SecurityContext 取，不查库），
 * 用户不存在的情况用 {@link BusinessException} 抛出并交由全局异常处理器统一转换，
 * 避免"有的地方返回 Result.error、有的地方抛异常"的不一致。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final NoteUserMapper noteUserMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile() {
        NoteUser user = requireCurrentUser();
        return Result.success(toDto(user));
    }

    @PutMapping("/profile")
    public Result<UserProfileDTO> updateProfile(@RequestBody Map<String, String> body) {
        NoteUser user = requireCurrentUser();
        if (body.containsKey("nickname")) {
            user.setNickname(body.get("nickname"));
        }
        if (body.containsKey("avatar")) {
            user.setAvatar(body.get("avatar"));
        }
        noteUserMapper.updateById(user);
        return Result.success(toDto(user));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        NoteUser user = requireCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        noteUserMapper.updateById(user);
        return Result.success();
    }

    private NoteUser requireCurrentUser() {
        Long userId = SecurityUtils.currentUserId();
        NoteUser user = noteUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已注销");
        }
        return user;
    }

    private UserProfileDTO toDto(NoteUser user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        return dto;
    }
}
