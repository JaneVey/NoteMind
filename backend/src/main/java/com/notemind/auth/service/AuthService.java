package com.notemind.auth.service;

import com.notemind.auth.dto.LoginRequest;
import com.notemind.auth.dto.LoginResponse;
import com.notemind.auth.dto.RegisterRequest;
import com.notemind.auth.util.JwtUtils;
import com.notemind.common.exception.BusinessException;
import com.notemind.entity.NoteUser;
import com.notemind.mapper.NoteUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final NoteUserMapper noteUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public void register(RegisterRequest request) {
        Long count = noteUserMapper.selectCount(
                new LambdaQueryWrapper<NoteUser>()
                        .eq(NoteUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        NoteUser user = new NoteUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        noteUserMapper.insert(user);
    }

    public LoginResponse login(LoginRequest request) {
        NoteUser user = noteUserMapper.selectOne(
                new LambdaQueryWrapper<NoteUser>()
                        .eq(NoteUser::getUsername, request.getUsername()));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        return response;
    }
}
