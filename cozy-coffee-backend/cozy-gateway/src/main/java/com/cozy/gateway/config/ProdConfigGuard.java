package com.cozy.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 生产环境 fail-closed 启动校验。
 * 规则：禁止 prod 与 local/test 混用；非 local/test 下 CORS 必须为明确的 https 白名单、
 * JWT_SECRET 必须 >=32 字节且不等于公开 fallback。
 */
@Component
public class ProdConfigGuard {

    private static final Logger log = LoggerFactory.getLogger(ProdConfigGuard.class);
    /** JwtUtil 中的公开 fallback，生产禁止使用 */
    static final String DEFAULT_JWT_SECRET = "cozy-coffee-dev-secret-key-change-in-production-32bytes";

    private final Environment environment;
    private final String allowedOrigins;
    private final String jwtSecret;

    public ProdConfigGuard(Environment environment,
            @Value("${cozy.web.allowed-origins:*}") String allowedOrigins) {
        this(environment, allowedOrigins, System.getenv("JWT_SECRET"));
    }

    /** 包内可见：测试用确定性注入 jwtSecret */
    ProdConfigGuard(Environment environment, String allowedOrigins, String jwtSecret) {
        this.environment = environment;
        this.allowedOrigins = allowedOrigins;
        this.jwtSecret = jwtSecret;
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

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.isEmpty() || origins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException(
                    "生产环境 CORS 不能为空或通配符：请设置 CORS_ALLOWED_ORIGINS 为明确域名白名单");
        }
        if (!origins.stream().allMatch(o -> o.startsWith("https://"))) {
            throw new IllegalStateException("生产环境 CORS 来源必须为 https 域名");
        }

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32
                || DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "生产环境必须设置长度>=32且非默认值的 JWT_SECRET 环境变量");
        }
        log.info("生产环境配置校验通过：CORS 白名单={}", origins);
    }
}
