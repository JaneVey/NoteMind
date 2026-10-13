package com.notemind.auth.service.impl;

import com.notemind.auth.dto.RegisterRequest;
import com.notemind.auth.service.TokenService;
import com.notemind.auth.token.LoginAttemptStore;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.framework.audit.OperationLogRecorder;
import com.notemind.framework.audit.SysOperationLog;
import com.notemind.framework.config.AuthProperties;
import com.notemind.user.entity.SysUser;
import com.notemind.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册与登出的审计留痕。
 *
 * <p>直接 new 出被测对象并注入 mock，不经过 Spring 代理 —— 否则 recorder 是 @Async 的，
 * 调用变异步就断言不到了。
 */
class AuthServiceImplAuditTest {

    private static final String CLIENT_IP = "203.0.113.7";
    private static final String USER_AGENT = "curl/8";
    private static final String PASSWORD = "pass1234";

    private SysUserMapper sysUserMapper;
    private TokenService tokenService;
    private OperationLogRecorder recorder;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        sysUserMapper = mock(SysUserMapper.class);
        tokenService = mock(TokenService.class);
        recorder = mock(OperationLogRecorder.class);
        service = new AuthServiceImpl(
                sysUserMapper,
                mock(PasswordEncoder.class),
                tokenService,
                mock(LoginAttemptStore.class),
                new AuthProperties(),
                recorder);
    }

    /** 取出本次唯一写入的那条审计 */
    private SysOperationLog captured() {
        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(recorder).record(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("登出记一条 LOGOUT，带上被吊销会话的 userId 与来源信息")
    void logoutIsAudited() {
        when(tokenService.revoke("raw-token")).thenReturn(9L);

        service.logout("raw-token", USER_AGENT, CLIENT_IP);

        SysOperationLog entry = captured();
        assertThat(entry.getOperation()).isEqualTo("LOGOUT");
        assertThat(entry.getModule()).isEqualTo("auth");
        assertThat(entry.getUserId()).isEqualTo(9L);
        assertThat(entry.getSuccess()).isTrue();
        assertThat(entry.getErrorCode()).isNull();
        assertThat(entry.getClientIp()).isEqualTo(CLIENT_IP);
        assertThat(entry.getUserAgent()).isEqualTo(USER_AGENT);
    }

    @Test
    @DisplayName("token 已失效时登出仍然留痕，userId 为 null")
    void logoutWithInvalidTokenStillAudited() {
        when(tokenService.revoke("stale")).thenReturn(null);

        service.logout("stale", USER_AGENT, CLIENT_IP);

        assertThat(captured().getUserId()).isNull();
    }

    @Test
    @DisplayName("注册成功后记一条 REGISTER，带新账号的 userId")
    void registerSuccessIsAudited() {
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        // 真实插入时 MyBatis-Plus 会把自增主键回填到实体上，这里模拟同一行为
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SysUser.class).setId(42L);
            return 1;
        });

        service.register(registerRequest(), USER_AGENT, CLIENT_IP);

        SysOperationLog entry = captured();
        assertThat(entry.getOperation()).isEqualTo("REGISTER");
        assertThat(entry.getUserId()).isEqualTo(42L);
        assertThat(entry.getSuccess()).isTrue();
    }

    @Test
    @DisplayName("注册撞重名时也留痕，并记下错误码 —— 枚举式注册探测就是靠这些记录发现的")
    void registerFailureIsAudited() {
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.register(registerRequest(), USER_AGENT, CLIENT_IP))
                .isInstanceOf(BusinessException.class);

        SysOperationLog entry = captured();
        assertThat(entry.getOperation()).isEqualTo("REGISTER");
        assertThat(entry.getSuccess()).isFalse();
        assertThat(entry.getErrorCode()).isEqualTo(ErrorCode.USERNAME_EXISTS.getCode());
        assertThat(entry.getUserId()).isNull();
    }

    @Test
    @DisplayName("审计里绝不出现密码")
    void auditNeverContainsPassword() {
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        service.register(registerRequest(), USER_AGENT, CLIENT_IP);

        assertThat(captured().getRequestParam())
                .contains("username=someone")
                .doesNotContain(PASSWORD);
    }

    private static RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("someone");
        request.setEmail("someone@example.com");
        request.setPassword(PASSWORD);
        return request;
    }
}
