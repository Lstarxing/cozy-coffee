package com.cozy.common.interceptor;

import com.cozy.common.context.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端鉴权拦截器
 * 验证用户是否具有管理员角色，仅允许管理员访问 /api/admin/* 路径
 */
@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 检查用户是否已登录
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
            return sendUnauthorized(response, "未登录，请先登录");
        }

        // 检查用户角色
        String role = UserContext.getRole();
        if (!"admin".equals(role)) {
            log.warn("AdminAuthInterceptor - 用户 {} 尝试访问管理端，角色: {}", userId, role);
            return sendForbidden(response, "权限不足，需要管理员权限");
        }

        log.debug("AdminAuthInterceptor - 管理员 {} 访问: {}", userId, request.getRequestURI());
        return true;
    }

    private boolean sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", 401);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false;
    }

    private boolean sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", 403);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false;
    }
}
