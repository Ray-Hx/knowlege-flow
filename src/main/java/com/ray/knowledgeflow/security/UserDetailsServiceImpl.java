package com.ray.knowledgeflow.security;

import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ray.knowledgeflow.module.user.entity.SysRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义 UserDetailsService
 *
 * @author Ray
 * @since 2024-01-14
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final IUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        // 加载用户角色权限
        List<SysRole> roles = userService.getUserRoles(user.getId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getRoleCode())));
        }

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                Integer.valueOf(1).equals(user.getStatus()));
    }
}
