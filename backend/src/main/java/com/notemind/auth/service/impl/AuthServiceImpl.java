package com.notemind.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.notemind.auth.dto.AuthResponse;
import com.notemind.auth.dto.AuthResult;
import com.notemind.auth.dto.LoginRequest;
import com.notemind.auth.dto.RegisterRequest;
import com.notemind.auth.service.AuthService;
import com.notemind.auth.service.TokenService;
import com.notemind.auth.token.IssuedTokens;
import com.notemind.auth.token.LoginAttemptStore;
import com.notemind.auth.token.LoginThrottlePolicy;
import com.notemind.auth.token.RefreshSession;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.framework.config.AuthProperties;
import com.notemind.framework.audit.OperationLogRecorder;
import com.notemind.framework.audit.SysOperationLog;
import com.notemind.user.entity.SysUser;
import com.notemind.user.entity.UserStatus;
import com.notemind.user.mapper.SysUserMapper;
import com.notemind.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 认证服务实现。
 *
 * <p>登录顺序有意义：来源限流 → 账号递进延迟 → 账号存在 → 有密码 → 密码匹配 → 未禁用。
 * 其中「账号不存在」必须与「密码错误」返回同一个错误码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String AUDIT_MODULE = "auth";

    private static final String AUDIT_OPERATION_LOGIN = "LOGIN";

    private static final String AUDIT_OPERATION_LOGOUT = "LOGOUT";

    private static final String AUDIT_OPERATION_REGISTER = "REGISTER";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginAttemptStore loginAttemptStore;
    private final AuthProperties authProperties;
    private final OperationLogRecorder operationLogRecorder;

    /**
     * 注册。
     *
     * <p>先查重再插入，并发下存在竞态；但 username 与 email 都有唯一索引，
     * 数据库会兜住，不会产生重复账号。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request, String userAgent, String clientIp) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            auditRegister(null, request, ErrorCode.USERNAME_EXISTS, clientIp, userAgent);
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // email 是 CITEXT（大小写不敏感唯一），等值比较即可，应用层不需要 lower()；
        // 前提是 JDBC URL 带了 stringtype=unspecified，否则 citext 会被 text 比较绕过
        Long emailCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, request.getEmail()));
        if (emailCount > 0) {
            auditRegister(null, request, ErrorCode.EMAIL_EXISTS, clientIp, userAgent);
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

        auditRegister(user.getId(), request, null, clientIp, userAgent);
        log.info("用户注册成功: userId={} username={}", user.getId(), user.getUsername());
    }

    @Override
    public AuthResult login(LoginRequest request, String userAgent, String clientIp) {
        String identifier = request.getIdentifier();
        AuthProperties.LoginThrottle throttle = authProperties.getLoginThrottle();
        Duration window = Duration.ofMinutes(throttle.getWindowMinutes());

        // 来源地址在窗口内失败过多则直接拒绝。放在核对密码之前，
        // 被拦下的请求不会消耗一次 BCrypt 计算
        if (loginAttemptStore.countIpFailures(clientIp) >= throttle.getIpMaxFailures()) {
            auditLogin(null, identifier, ErrorCode.LOGIN_TOO_FREQUENT, clientIp, userAgent);
            log.warn("登录被拒（来源地址失败次数过多）: ip={}", clientIp);
            throw new BusinessException(ErrorCode.LOGIN_TOO_FREQUENT);
        }

        // 该账号已多次失败则先延迟。正确密码同样要等 —— 延迟惩罚的是反复试错这个行为
        applyFailureDelay(identifier, throttle);

        SysUser user = findByIdentifier(identifier);

        // 账号不存在 与 密码不匹配 用同一个错误码，不泄露"该账号是否已注册"
        if (user == null) {
            recordFailure(identifier, clientIp, window);
            auditLogin(null, identifier, ErrorCode.LOGIN_FAILED, clientIp, userAgent);
            log.warn("登录失败（账号不存在）: identifier={} ip={}", identifier, clientIp);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 第三方登录注册的账号没有密码 —— 必须明确告知，否则用户会以为自己密码打错了
        if (user.getPassword() == null) {
            auditLogin(user.getId(), identifier, ErrorCode.OAUTH_ACCOUNT_ONLY, clientIp, userAgent);
            log.info("密码登录被拒（该账号仅支持第三方登录）: userId={}", user.getId());
            throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_ONLY);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailure(identifier, clientIp, window);
            auditLogin(user.getId(), identifier, ErrorCode.LOGIN_FAILED, clientIp, userAgent);
            log.warn("登录失败（密码错误）: userId={} ip={}", user.getId(), clientIp);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            auditLogin(user.getId(), identifier, ErrorCode.ACCOUNT_DISABLED, clientIp, userAgent);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 登录成功才清账号计数；IP 计数不清，它衡量的是来源地址而非账号
        loginAttemptStore.clearIdentifierFailures(identifier);
        auditLogin(user.getId(), identifier, null, clientIp, userAgent);

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
    public void logout(String refreshToken, String userAgent, String clientIp) {
        Long userId = tokenService.revoke(refreshToken);
        auditLogout(userId, clientIp, userAgent);
        // 不记录 token 内容，只说明发生了一次登出。userId 为 null 表示 token 已失效或不存在
        log.info("用户登出: userId={}", userId);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /**
     * 按"用户名或邮箱"查用户。两个字段都是 CITEXT 唯一索引，大小写不敏感。
     *
     * <p>不会查出两条：用户名字符集 {@code [a-zA-Z0-9_-]} 不含 {@code @}，邮箱必然含 {@code @}。
     */
    private SysUser findByIdentifier(String identifier) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, identifier)
                        .or()
                        .eq(SysUser::getEmail, identifier));
    }

    /**
     * 记一次凭据失败。账号维度与来源地址维度各记一次：
     * 前者用于递进延迟，后者用于限流。
     */
    private void recordFailure(String identifier, String clientIp, Duration window) {
        loginAttemptStore.recordIdentifierFailure(identifier, window);
        loginAttemptStore.recordIpFailure(clientIp, window);
    }

    /** 该账号失败次数已达阈值时先延迟再继续。延迟不改变登录结果，只惩罚反复试错 */
    private void applyFailureDelay(String identifier, AuthProperties.LoginThrottle throttle) {
        long failures = loginAttemptStore.countIdentifierFailures(identifier);
        long delay = LoginThrottlePolicy.delayMillis(
                (int) failures,
                throttle.getAccountDelayThreshold(),
                throttle.getBaseDelayMillis(),
                throttle.getMaxDelayMillis());
        if (delay <= 0) {
            return;
        }

        log.warn("账号失败次数偏多，延迟 {}ms 后继续: identifier={}", delay, identifier);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // 恢复中断标记后继续：延迟只是防护措施，不该因线程被中断而改变登录结果
            Thread.currentThread().interrupt();
        }
    }

    private void auditLogin(Long userId, String identifier, ErrorCode errorCode,
                            String clientIp, String userAgent) {
        record(AUDIT_OPERATION_LOGIN, userId, "identifier=" + identifier, errorCode, clientIp, userAgent);
    }

    /** 记一条登出审计。{@code userId} 为 null 表示 token 已失效或不存在 —— 那本身也值得留痕 */
    private void auditLogout(Long userId, String clientIp, String userAgent) {
        record(AUDIT_OPERATION_LOGOUT, userId, null, null, clientIp, userAgent);
    }

    private void auditRegister(Long userId, RegisterRequest request, ErrorCode errorCode,
                               String clientIp, String userAgent) {
        record(AUDIT_OPERATION_REGISTER, userId,
                "username=" + request.getUsername() + " email=" + request.getEmail(),
                errorCode, clientIp, userAgent);
    }

    /**
     * 装配一条操作日志并交给异步写入方。
     *
     * <p>{@code userId} 允许为 null（账号不存在 / token 已失效时也要留痕，那正是被攻击时
     * 才有价值的记录）。{@code requestParam} 只放账号标识，**绝不放密码**。
     */
    private void record(String operation, Long userId, String requestParam,
                        ErrorCode errorCode, String clientIp, String userAgent) {
        SysOperationLog entry = new SysOperationLog();
        entry.setUserId(userId);
        entry.setModule(AUDIT_MODULE);
        entry.setOperation(operation);
        entry.setSuccess(errorCode == null);
        entry.setErrorCode(errorCode == null ? null : errorCode.getCode());
        entry.setErrorMessage(errorCode == null ? null : errorCode.getMessage());
        entry.setRequestParam(requestParam);
        entry.setClientIp(clientIp);
        entry.setUserAgent(userAgent);
        operationLogRecorder.record(entry);
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
