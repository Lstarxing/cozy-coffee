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
        String token = null;

        // 1. 先从 Authorization Header 读取
        String authHeader = request.getHeader("Authorization");
        log.debug("JwtAuthInterceptor - Request URI: {}, Authorization header: {}",
                request.getRequestURI(),
                authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "null");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2. 再从 Cookie 读取（httpOnly cookie fallback）
        if (token == null) {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie c : cookies) {
                    if ("cozy_token".equals(c.getName())) {
                        token = c.getValue();
                        break;
                    }
                }
            }
        }

        // 无 token：匿名访问放行
        if (token == null) {
            log.debug("JwtAuthInterceptor - No Bearer token or cookie, anonymous access");
            return true;
        }
        try {
            // 验证 token，无效直接拒绝（区别于匿名访问）
            if (!JwtUtil.validateToken(token)) {
                log.warn("JwtAuthInterceptor - Invalid token, reject: uri={}", request.getRequestURI());
                return sendUnauthorized(response, "无效的登录凭证");
            }

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
            return true;
        } catch (Exception e) {
            log.warn("JwtAuthInterceptor - Token parse error: {}", e.getMessage());
            return sendUnauthorized(response, "登录凭证解析失败");
        }
    }

    private boolean sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        if (response.isCommitted()) {
            return false;
        }
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
