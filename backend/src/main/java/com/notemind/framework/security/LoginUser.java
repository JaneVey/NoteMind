package com.notemind.framework.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 当前登录用户的主体（SecurityContext 的 principal）。
 *
 * <p><b>变更记录（2026-10-07）</b>：此前本类只接受 {@code NoteUser} 实体构造，
 * 但全项目无任何地方使用它（死代码）——实际链路是过滤器往 SecurityContext 里
 * 放入一个裸 {@code Long}，再由各 Controller 强转取值。现已改为直接由 JWT 声明构造：
 *
 * <pre>
 *   new LoginUser(userId, username)
 * </pre>
 *
 * <p><b>为什么不持有密码与昵称</b>：本项目采用 JWT 无状态鉴权，
 * 每次请求不查库也能拿到 userId 与 username（两者都在 token 里）。
 * 这避免了"每个请求多一次数据库查询"的开销。
 * 需要昵称 / 头像 / 邮箱时，按 userId 查询 {@code note_user} 表即可。
 */
public class LoginUser implements UserDetails {

    private final Long id;
    private final String username;

    public LoginUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * JWT 无状态鉴权不持有密码，此处固定返回 {@code null}。
     * 本项目未使用 DaoAuthenticationProvider，该值不会被读取。
     */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
