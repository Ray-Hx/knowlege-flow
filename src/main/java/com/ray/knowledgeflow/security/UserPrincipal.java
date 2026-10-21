package com.ray.knowledgeflow.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证主体，在 UserDetails 基础上携带用户 ID。
 *
 * <p>携带 ID 的目的是让 {@link SecurityUtils#getCurrentUserId()} 直接从 SecurityContext 取值，
 * 避免每次调用都按 username 回表查询。
 *
 * @author Ray
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public UserPrincipal(Long id, String username, String password,
            Collection<? extends GrantedAuthority> authorities,
            boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    /**
     * 由 JWT 声明构建，不含密码（无需校验密码的场景）。
     */
    public static UserPrincipal fromClaims(Long id, String username, List<String> roleCodes) {
        List<SimpleGrantedAuthority> authorities = roleCodes == null ? List.of()
                : roleCodes.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return new UserPrincipal(id, username, null, authorities, true);
    }

    /**
     * 角色编码列表，用于写入 JWT 声明。
     */
    public List<String> getRoleCodes() {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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
        return enabled;
    }
}
