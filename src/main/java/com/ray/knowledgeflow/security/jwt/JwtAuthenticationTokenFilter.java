package com.ray.knowledgeflow.security.jwt;

import com.ray.knowledgeflow.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * <p>认证主体完全由令牌声明构建，不查询数据库。代价是用户被禁用或角色变更后，
 * 旧令牌在过期前仍然有效；如需即时失效需引入令牌黑名单。
 *
 * @author Ray
 * @since 2024-01-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    /** 令牌校验失败原因，供 {@link JwtAuthenticationEntryPoint} 返回具体错误 */
    public static final String ATTR_AUTH_ERROR = "jwt.auth.error";

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jwt = authHeader.substring(7);
            try {
                Claims claims = jwtUtils.parseClaims(jwt);
                Long userId = jwtUtils.getUserIdFromClaims(claims);

                if (userId == null) {
                    // 认证改造前签发的旧令牌不含 uid，无法构建主体
                    request.setAttribute(ATTR_AUTH_ERROR, "登录状态已失效，请重新登录");
                } else {
                    UserPrincipal principal = UserPrincipal.fromClaims(
                            userId, claims.getSubject(), jwtUtils.getRolesFromClaims(claims));

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(ATTR_AUTH_ERROR, "登录已过期，请重新登录");
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT 校验失败: {}", e.getMessage());
                request.setAttribute(ATTR_AUTH_ERROR, "登录凭证无效");
            }
        }

        chain.doFilter(request, response);
    }
}
