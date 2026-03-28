package com.cozy.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT工具类 (兼容JJWT 0.11.5)
 */
public class JwtUtil {

    // 密钥（生产环境应从配置中心读取）
    private static final String SECRET_KEY = "cozy-coffee-secret-key-for-jwt-token-generation-32bytes";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // Token有效期：7天
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;

    public static long getExpirationTimeMillis() {
        return EXPIRATION_TIME;
    }

    /**
     * 生成JWT Token（包含角色信息和Token版本号）
     */
    public static String generateToken(Long userId, String username, String role, Integer tokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("role", role != null ? role : "user")
                .claim("tokenVersion", tokenVersion != null ? tokenVersion : 0)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成JWT Token（向后兼容，不传tokenVersion）
     */
    public static String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, 0);
    }

    /**
     * 生成JWT Token（向后兼容，默认role为user）
     */
    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, "user");
    }

    /**
     * 解析JWT Token
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从Token中获取用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从Token中获取用户角色
     */
    public static String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        String role = claims.get("role", String.class);
        return role != null ? role : "user";
    }

    /**
     * 从Token中获取Token版本号
     */
    public static Integer getTokenVersionFromToken(String token) {
        Claims claims = parseToken(token);
        Integer version = claims.get("tokenVersion", Integer.class);
        return version != null ? version : 0;
    }

    /**
     * 验证Token是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
