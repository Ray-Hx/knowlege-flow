package com.ray.knowledgeflow.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.knowledgeflow.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证入口：统一返回 401 与具体原因。
 *
 * <p>没有它时，令牌过期的请求会以匿名身份走到授权阶段并被拒为 403，
 * 前端无法区分“需要刷新令牌”和“确实无权限”。
 *
 * @author Ray
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        Object reason = request.getAttribute(JwtAuthenticationTokenFilter.ATTR_AUTH_ERROR);
        String message = reason instanceof String ? (String) reason : "请先登录";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Result.fail(401, message));
    }
}
