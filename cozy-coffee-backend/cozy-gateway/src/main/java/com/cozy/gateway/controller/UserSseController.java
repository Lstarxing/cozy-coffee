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
 * 用户端 SSE 控制器
 * 用于向用户推送订单状态变更等实时通知
 */
@Slf4j
@RestController
@RequestMapping("/api/member/sse")
@RequiredArgsConstructor
public class UserSseController {

    private final SseTicketService ticketService;
    private final SseConnectionManager connectionManager;

    /**
     * 获取 SSE 连接票据
     * 需要先通过 JWT 认证
     */
    @PostMapping("/ticket")
    public Result<Map<String, String>> getTicket(HttpServletRequest request) {
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
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
        Long userId = ticketService.validateAndConsumeTicket(ticket);
        if (userId == null) {
            log.warn("用户 SSE 连接失败：无效的 Ticket");
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

        log.info("用户 SSE 连接建立: userId={}", userId);
        return connectionManager.createConnection(userId);
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
