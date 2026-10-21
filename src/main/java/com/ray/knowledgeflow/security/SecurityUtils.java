package com.ray.knowledgeflow.security;

import com.ray.knowledgeflow.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 *
 * <p>全部取值来自 SecurityContext，不访问数据库。
 *
 * @author Ray
 * @since 2024-01-16
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    private static UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "用户未登录");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new BusinessException(401, "用户未登录");
        }
        return (UserPrincipal) principal;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        return currentPrincipal().getUsername();
    }

    /**
     * 获取当前登录用户 ID
     */
    public static Long getCurrentUserId() {
        return currentPrincipal().getId();
    }
}
