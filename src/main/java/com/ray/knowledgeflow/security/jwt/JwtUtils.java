package com.ray.knowledgeflow.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author Ray
 * @since 2024-01-14
 */
@Slf4j
@Component
public class JwtUtils {

    /** 用户 ID 声明 */
    public static final String CLAIM_USER_ID = "uid";
    /** 角色编码声明 */
    public static final String CLAIM_ROLES = "roles";

    /**
     * 签名密钥。不提供默认值：缺失时应用启动即失败，避免静默使用硬编码密钥。
     */
    @Value("${jwt.secret}")
    private String secret;

    /** 过期时间，毫秒 */
    @Value("${jwt.expiration}")
    private Long expiration;

    @PostConstruct
    void initializeSecret() {
        if (secret == null || secret.isBlank()) {
            byte[] randomKey = new byte[32];
            new SecureRandom().nextBytes(randomKey);
            secret = Base64.getEncoder().encodeToString(randomKey);
            log.warn("JWT_SECRET 未配置，已为当前进程生成随机开发密钥；重启后现有会话将失效");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 至少需要 32 字节");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成令牌，并将用户 ID 与角色写入声明，使后续请求无需回表。
     */
    public String generateToken(Long userId, String username, List<String> roleCodes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_ROLES, roleCodes);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析并验签，令牌无效或过期时抛出 JwtException。
     */
    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromClaims(Claims claims) {
        Number userId = claims.get(CLAIM_USER_ID, Number.class);
        return userId == null ? null : userId.longValue();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromClaims(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        return roles instanceof List ? (List<String>) roles : List.of();
    }
}
