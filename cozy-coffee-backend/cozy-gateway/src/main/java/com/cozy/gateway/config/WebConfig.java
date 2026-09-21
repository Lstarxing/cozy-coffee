package com.cozy.gateway.config;

import com.cozy.common.interceptor.AdminAuthInterceptor;
import com.cozy.common.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web配置类
 * 注册拦截器和跨域配置
 *
 * ⚠️ 这里是【唯一】的拦截器注册点（2026-09-21 合并）。
 *    此前 `cozy-common/.../config/WebMvcConfig` 也注册了同一对拦截器，而
 *    InterceptorRegistry 是【累加】的 —— 两处都注册，实际生效的放行名单就成了两份的
 *    【交集】，只在一边加路径等于没加，且每个请求会被拦截两次。
 *    表现：「管理端密码正确也永远登不进」—— /api/auth/admin/login 只补在网关这边，
 *    被 common 那份漏掉的白名单挡死。合并后这种隐性规则不复存在。
 *    `/api/**` 的公开端点、SSE 与管理员权限都是**网关入口策略**，故注册规则归本模块；
 *    拦截器实现仍留在 cozy-common 供其他服务复用。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Value("${storage.local-upload-dir:./uploads}")
    private String localUploadDir;

    @Value("${cozy.web.allowed-origins:*}")
    private String allowedOrigins;

    /** 测试端点是否放行；默认 false（fail-closed），仅本地/测试 profile 显式打开 */
    @Value("${cozy.security.exclude-test-paths:false}")
    private boolean excludeTestPaths;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(localUploadDir).toAbsolutePath().normalize();
        // 用 toUri() 生成规范 file:/// 前缀，Windows 下 "file:C:/..." 会导致资源处理器抛异常
        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
        registry.addResourceHandler("/api/uploads/**").addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT拦截器 —— 放行名单必须【穷举所有不需要会话的入口】。
        // JwtAuthInterceptor 的判定顺序：无 token → 匿名放行；带了 token 但无效 → 【直接 401】，
        // 不看该路径是否本就公开。所以漏一个公开入口，客户端手里一个失效 token 就把自己锁死 ——
        // 密码完全正确也进不去，且无法自愈。
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 公开 auth 入口：注册 / 用户端登录 / 管理端登录 / 小程序静默登录 / dev 重置
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/admin/login",
                        "/api/auth/wechat/**",
                        "/api/auth/password/reset-dev",
                        // 测试端点：默认【不放行】，由 cozy.security.exclude-test-paths 显式打开
                        excludeTestPaths ? "/api/auth/test" : "",
                        excludeTestPaths ? "/api/member/test" : "",
                        // SSE 长连接不能被 JWT 拦截器打断（见 d6d2028）。
                        // ⚠️ 只放行 /events —— 它凭一次性 ticket 连接，本身不读 UserContext；
                        //    而 /ticket 与 /disconnect 都调 AuthUtil.requireUserId()，必须先经拦截器
                        //    把 UserContext 填好，放行它们反而会让它们失效。
                        "/api/admin/sse/events",
                        "/api/member/sse/events");

        // 管理端拦截器 - 仅拦截管理端API；SSE 的 /events 同上（建连时没有 JWT 可用）
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/sse/events");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
