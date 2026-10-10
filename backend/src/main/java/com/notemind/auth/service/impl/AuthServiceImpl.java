package com.notemind.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.notemind.auth.dto.AuthResponse;
import com.notemind.auth.dto.AuthResult;
import com.notemind.auth.dto.LoginRequest;
import com.notemind.auth.dto.RegisterRequest;
import com.notemind.auth.service.AuthService;
import com.notemind.auth.service.TokenService;
import com.notemind.auth.token.IssuedTokens;
import com.notemind.auth.token.RefreshSession;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.user.entity.SysUser;
import com.notemind.user.entity.UserStatus;
import com.notemind.user.mapper.SysUserMapper;
import com.notemind.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 认证服务实现。
 *
 * <h3>登录流程中的四个检查（顺序有意义）</h3>
 * <ol>
 *   <li>账号是否存在 —— <b>与密码错误返回同一个错误码</b>，避免暴露"该用户名已注册"</li>
 *   <li>是否有密码 —— 第三方登录注册的账号 {@code password} 为 null，
 *       必须明确提示"请用 GitHub 登录"，而不是含糊的"用户名或密码错误"</li>
 *   <li>密码是否匹配</li>
 *   <li>账号是否被禁用</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * 注册。
     *
     * <p>这里**先查重再插入**，并发下存在竞态（两个请求同时通过查重）。
     * 但 {@code sys_user.username} 与 {@code email} 都有唯一索引，
     * 因此即使竞态发生，数据库仍会兜住，不会产生重复账号。
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
        // （前提是 JDBC URL 带了 stringtype=unspecified，否则 citext 会被 text 比较绕过）
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
        // emailVerified / status 由数据库默认值给出（false / 'active'）
        sysUserMapper.insert(user);

        // TODO 新用户初始化数据（默认笔记本、预设 Prompt 快捷指令、用户画像）
        //  当前种子数据只挂在 admin(user_id=1) 名下，导致每个新注册用户登录后应用是空的。
        //  待对应业务表设计时一并处理。

        log.info("用户注册成功: userId={} username={}", user.getId(), user.getUsername());
    }

    @Override
    public AuthResult login(LoginRequest request, String userAgent, String clientIp) {
        SysUser user = findByIdentifier(request.getIdentifier());

        // ① 账号不存在 与 ② 密码不匹配 → 同一个错误码，不给攻击者提供"账号是否存在"的信息
        if (user == null) {
            log.warn("登录失败（账号不存在）: identifier={} ip={}", request.getIdentifier(), clientIp);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 第三方登录注册的账号没有密码 —— 必须明确告知，否则用户会一直以为自己密码打错了
        if (user.getPassword() == null) {
            log.info("密码登录被拒（该账号仅支持第三方登录）: userId={}", user.getId());
            throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_ONLY);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败（密码错误）: userId={} ip={}", user.getId(), clientIp);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
        IssuedTokens tokens = tokenService.issue(
                user.getId(), user.getUsername(), rememberMe, userAgent, clientIp);

        touchLastLogin(user.getId());
        log.info("登录成功: userId={} username={} rememberMe={} ip={}",
                user.getId(), user.getUsername(), rememberMe, clientIp);

        return new AuthResult(toBody(tokens, user), tokens.refreshToken(), tokens.refreshTtl());
    }

    @Override
    public AuthResult refresh(String refreshToken, String userAgent, String clientIp) {
        // consume 内部会做重用检测；检测到重用会吊销该用户全部会话并抛异常
        RefreshSession session = tokenService.consume(refreshToken);

        SysUser user = sysUserMapper.selectById(session.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 沿用原会话的 rememberMe —— refresh 请求没有 body，无法从请求中得知
        IssuedTokens tokens = tokenService.issue(
                user.getId(), user.getUsername(), session.rememberMe(), userAgent, clientIp);

        return new AuthResult(toBody(tokens, user), tokens.refreshToken(), tokens.refreshTtl());
    }

    @Override
    public void logout(String refreshToken) {
        tokenService.revoke(refreshToken);
        // 不记录 token 内容，只说明发生了一次登出
        log.info("用户登出");
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /**
     * 按"用户名或邮箱"查用户。
     *
     * <p>两个字段都是唯一索引且都是 CITEXT，所以大小写不敏感。
     *
     * <p>为什么不会查出两条：用户名的可用字符是 {@code [a-zA-Z0-9_-]}（注册时校验），
     * <b>不含 {@code @}</b>，而邮箱必然含 {@code @} —— 二者取值集合不相交，
     * 因此不存在"A 的用户名等于 B 的邮箱"的情况。
     */
    private SysUser findByIdentifier(String identifier) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, identifier)
                        .or()
                        .eq(SysUser::getEmail, identifier));
    }

    /** 记录最近登录时间（登录审计）。只更新这一个字段 */
    private void touchLastLogin(Long userId) {
        SysUser patch = new SysUser();
        patch.setId(userId);
        patch.setLastLoginAt(OffsetDateTime.now());
        sysUserMapper.updateById(patch);
    }

    private AuthResponse toBody(IssuedTokens tokens, SysUser user) {
        return new AuthResponse(tokens.accessToken(), tokens.expiresIn(), toVO(user));
    }

    /** Entity → VO。绝不把含 password 的实体直接暴露出去 */
    private UserProfileVO toVO(SysUser user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setEmailVerified(user.getEmailVerified());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
