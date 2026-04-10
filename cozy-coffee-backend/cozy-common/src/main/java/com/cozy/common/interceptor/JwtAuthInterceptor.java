package com.cozy.common.interceptor;

import com.cozy.common.context.UserContext;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT 鉴权拦截器
 * 统一处理 token 验证，将用户ID和角色放入上下文
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 获取 Authorization Header
        String authHeader = request.getHeader("Authorization");
        log.debug("JwtAuthInterceptor - Request URI: {}, Authorization header: {}",
                request.getRequestURI(),
                authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "null");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 验证 token 并提取用户ID和角色
                if (JwtUtil.validateToken(token)) {
                    if (stringRedisTemplate != null) {
                        String sessionKey = RedisKeyConstants.userLoginSession(token);
                        String cachedUserId = stringRedisTemplate.opsForValue().get(sessionKey);
                        if (cachedUserId == null || cachedUserId.isBlank()) {
                            log.warn("JwtAuthInterceptor - Session missing in Redis, reject token: uri={}",
                                    request.getRequestURI());
                            return sendUnauthorized(response, "登录已失效，请重新登录");
                        }
                    }

                    Long userId = JwtUtil.getUserIdFromToken(token);
                    String role = JwtUtil.getRoleFromToken(token);
                    // 将用户ID和角色放入上下文，并设置到请求属性中以兼容某些控制器
                    UserContext.setUserId(userId);
                    UserContext.setRole(role);
                    request.setAttribute("userId", userId);
                    log.debug("JwtAuthInterceptor - Token valid, userId: {}, role: {}", userId, role);
                } else {
                    log.warn("JwtAuthInterceptor - Token validation failed");
                }
            } catch (Exception e) {
                log.warn("JwtAuthInterceptor - Token parse error: {}", e.getMessage());
            }
        } else {
            log.debug("JwtAuthInterceptor - No Bearer token found");
        }

        return true; // 继续执行，由具体接口决定是否需要登录
    }

    private boolean sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", 401);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 请求结束，清除上下文
        UserContext.clear();
    }
}
