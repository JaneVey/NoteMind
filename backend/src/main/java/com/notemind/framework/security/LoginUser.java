package com.notemind.framework.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 当前登录用户的主体（SecurityContext 的 principal）。
 *
 * <p>由 JWT 声明直接构造，只持有 userId 与 username —— 两者都在 token 里，
 * 因此每个请求不必查库。需要昵称 / 头像 / 邮箱时按 userId 另查。
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
