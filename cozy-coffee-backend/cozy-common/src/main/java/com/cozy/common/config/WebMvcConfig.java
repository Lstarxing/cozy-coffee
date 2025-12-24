package com.cozy.common.config;

import com.cozy.common.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**") // 拦截所有 API 请求
                .excludePathPatterns(
                        "/api/auth/login", // 排除登录
                        "/api/auth/register", // 排除注册
                        "/api/auth/test", // 排除测试
                        "/api/member/test" // 排除测试
                );
    }
}
