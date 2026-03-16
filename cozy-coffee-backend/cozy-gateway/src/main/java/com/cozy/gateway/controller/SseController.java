package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.common.context.UserContext;
import com.cozy.gateway.sse.SseConnectionManager;
import com.cozy.gateway.sse.SseTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * SSE 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseTicketService ticketService;
    private final SseConnectionManager connectionManager;

    /**
     * 获取 SSE 连接票据
     * 需要先通过 JWT 认证
     */
    @PostMapping("/ticket")
    public Result<Map<String, String>> getTicket(HttpServletRequest request) {
        // 优先从 UserContext 获取（已由 JwtAuthInterceptor 设置）
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
            // 兼容性回退：尝试从请求属性获取
            userId = (Long) request.getAttribute("userId");
        }

        if (userId == null) {
            return Result.error("请先登录");
        }

        String ticket = ticketService.generateTicket(userId);
        return Result.success(Map.of("ticket", ticket));
    }

    /**
     * 建立 SSE 连接
     * 使用 Ticket 验证（不需要 Authorization 头）
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String ticket) {
        // 验证票据
        Long userId = ticketService.validateAndConsumeTicket(ticket);
        if (userId == null) {
            log.warn("SSE 连接失败：无效的 Ticket");
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"message\":\"无效或过期的连接票据\"}"));
                emitter.complete();
            } catch (Exception e) {
                // 忽略
            }
            return emitter;
        }

        // 创建连接
        return connectionManager.createConnection(userId);
    }

    /**
     * 获取当前连接状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        return Result.success(Map.of(
                "connections", connectionManager.getConnectionCount()));
    }

    /**
     * 断开连接
     */
    @PostMapping("/disconnect")
    public Result<Void> disconnect(HttpServletRequest request) {
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
            userId = (Long) request.getAttribute("userId");
        }

        if (userId != null) {
            connectionManager.removeConnection(userId);
        }
        return Result.success(null);
    }
}
