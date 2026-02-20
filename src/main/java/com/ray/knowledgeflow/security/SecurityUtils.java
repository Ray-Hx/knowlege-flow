package com.ray.knowledgeflow.security;

import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.service.IUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 安全工具类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Component
public class SecurityUtils {

    private static IUserService userService;

    public SecurityUtils(IUserService userService) {
        SecurityUtils.userService = userService;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "用户未登录");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    /**
     * 获取当前登录用户 ID
     */
    public static Long getCurrentUserId() {
        String username = getCurrentUsername();
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return user.getId();
    }

    /**
     * 获取当前登录用户
     */
    public static SysUser getCurrentUser() {
        String username = getCurrentUsername();
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return user;
    }
}
