package com.notemind.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.notemind.auth.dto.LoginRequest;
import com.notemind.auth.dto.LoginResponse;
import com.notemind.auth.dto.RegisterRequest;
import com.notemind.auth.service.AuthService;
import com.notemind.auth.util.JwtUtils;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.user.entity.SysUser;
import com.notemind.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * 注册。
     *
     * <p>注意：这里**先查重再插入**，在并发下存在竞态（两个请求同时通过查重）。
     * 严格做法是依赖数据库唯一索引并捕获 {@code DuplicateKeyException}。
     * {@code sys_user.username} 已建唯一索引，因此即使竞态发生，数据库仍会兜住，
     * 不会产生重复用户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // email 在数据库里是 CITEXT（大小写不敏感唯一），等值比较即可 ——
        // Admin@x.com 与 admin@x.com 会被正确判定为同一个邮箱，应用层不需要手动 lower()
        Long emailCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, request.getEmail()));
        if (emailCount > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null
                ? request.getNickname()
                : request.getUsername());
        // emailVerified / status 由数据库默认值给出（false / 'active'），此处不显式设置，
        // 避免在 Java 里散落 'active' 这类魔法字符串
        sysUserMapper.insert(user);

        // TODO 新用户初始化数据（默认笔记本、预设 Prompt 快捷指令、用户画像、AI 供应商配置）
        //  待数据库重构定稿后实现 —— 当前种子数据只挂在 admin(user_id=1) 名下，
        //  导致每个新注册用户登录后应用是空的。详见《04-数据库设计规范》。

        log.info("用户注册成功: userId={} username={}", user.getId(), user.getUsername());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername()));

        // 注意：用户不存在与密码错误返回同一个错误码，避免暴露"该用户名已注册"
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败: username={}", request.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        log.info("登录成功: userId={} username={}", user.getId(), user.getUsername());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        return response;
    }
}
