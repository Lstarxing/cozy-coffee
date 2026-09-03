package com.cozy.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * 生产环境 fail-closed 启动校验。
 * 规则：禁止 prod 与 local/test 混用；非 local/test 下 CORS 必须为明确的 https 白名单、
 * JWT_SECRET 必须 >=32 字节且不等于公开 fallback。
 * 演示豁免：cozy.guard.allow-insecure-http=true 时允许显式 http 白名单（无域名 http://IP 场景）；
 * 仍拒绝通配符/空白名单/默认 JWT/dev-login。
 */
@Component
public class ProdConfigGuard {

    private static final Logger log = LoggerFactory.getLogger(ProdConfigGuard.class);
    /** JwtUtil 中的公开 fallback，生产禁止使用 */
    static final String DEFAULT_JWT_SECRET = "cozy-coffee-dev-secret-key-change-in-production-32bytes";

    private final Environment environment;
    private final AuthProperties authProperties;
    private final String allowedOrigins;
    private final String jwtSecret;
    private final boolean allowInsecureHttp;

    @Autowired
    public ProdConfigGuard(Environment environment,
            AuthProperties authProperties,
            @Value("${cozy.web.allowed-origins:*}") String allowedOrigins,
            @Value("${cozy.guard.allow-insecure-http:false}") boolean allowInsecureHttp) {
        this(environment, authProperties, allowedOrigins, System.getenv("JWT_SECRET"), allowInsecureHttp);
    }

    /** 包内可见：测试用确定性注入 jwtSecret */
    ProdConfigGuard(Environment environment, String allowedOrigins, String jwtSecret) {
        this(environment, new AuthProperties(), allowedOrigins, jwtSecret, false);
    }

    /** 包内可见：测试生产环境的认证开关 */
    ProdConfigGuard(Environment environment, AuthProperties authProperties, String allowedOrigins, String jwtSecret) {
        this(environment, authProperties, allowedOrigins, jwtSecret, false);
    }

    /** 包内可见：测试用，可显式开启 http 白名单豁免（无域名 http://IP 演示） */
    ProdConfigGuard(Environment environment, AuthProperties authProperties, String allowedOrigins, String jwtSecret, boolean allowInsecureHttp) {
        this.environment = environment;
        this.authProperties = authProperties;
        this.allowedOrigins = allowedOrigins;
        this.jwtSecret = jwtSecret;
        this.allowInsecureHttp = allowInsecureHttp;
    }

    @PostConstruct
    public void validate() {
        String[] profiles = environment.getActiveProfiles();
        boolean hasLocal = Arrays.stream(profiles).anyMatch("local"::equals);
        boolean hasTest = Arrays.stream(profiles).anyMatch("test"::equals);
        boolean hasProd = Arrays.stream(profiles).anyMatch("prod"::equals);

        if (hasProd && (hasLocal || hasTest)) {
            throw new IllegalStateException("禁止 prod 与 local/test profile 同时激活");
        }
        if (hasLocal || hasTest) {
            return; // 本地/测试跳过
        }

        if (authProperties.isDevLoginEnabled()) {
            throw new IllegalStateException("非 local/test 环境禁止开启开发登录和开发密码重置");
        }

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.isEmpty() || origins.stream().anyMatch(o -> o.contains("*"))) {
            throw new IllegalStateException(
                    "生产环境 CORS 不能为空或通配符：请设置 CORS_ALLOWED_ORIGINS 为明确域名白名单");
        }
        if (!origins.stream().allMatch(this::isAllowedOrigin)) {
            throw new IllegalStateException(allowInsecureHttp
                    ? "生产环境 CORS 来源必须为显式的 http(s) 白名单"
                    : "生产环境 CORS 来源必须为 https 域名（http://IP 演示可设 cozy.guard.allow-insecure-http=true）");
        }

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32
                || DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "生产环境必须设置长度>=32且非默认值的 JWT_SECRET 环境变量");
        }
        log.info("生产环境配置校验通过：CORS 白名单={}", origins);
    }

    private boolean isAllowedOrigin(String origin) {
        return isExplicitHttpsOrigin(origin)
                || (allowInsecureHttp && isExplicitHttpOrigin(origin));
    }

    private boolean isExplicitHttpOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String path = uri.getPath();
            return "http".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null
                    && uri.getQuery() == null
                    && uri.getFragment() == null
                    && (path == null || path.isEmpty() || "/".equals(path));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isExplicitHttpsOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String path = uri.getPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null
                    && uri.getQuery() == null
                    && uri.getFragment() == null
                    && (path == null || path.isEmpty() || "/".equals(path));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
