package com.notemind.user.controller;

import com.notemind.common.result.Result;
import com.notemind.entity.NoteUser;
import com.notemind.mapper.NoteUserMapper;
import com.notemind.user.dto.UpdatePasswordRequest;
import com.notemind.user.dto.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final NoteUserMapper noteUserMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        NoteUser user = noteUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        return Result.success(dto);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        NoteUser user = noteUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (body.containsKey("nickname")) {
            user.setNickname(body.get("nickname"));
        }
        if (body.containsKey("avatar")) {
            user.setAvatar(body.get("avatar"));
        }
        noteUserMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody UpdatePasswordRequest request,
                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        NoteUser user = noteUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return Result.error(400, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        noteUserMapper.updateById(user);
        return Result.success();
    }
}
