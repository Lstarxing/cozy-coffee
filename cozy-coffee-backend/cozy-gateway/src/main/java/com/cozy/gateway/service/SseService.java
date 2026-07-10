package com.cozy.gateway.service;

import com.cozy.common.context.UserContext;
import com.cozy.gateway.sse.SseConnectionManager;
import com.cozy.gateway.sse.SseTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 连接服务 — 票据管理 + 连接建立/断开。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final SseTicketService ticketService;
    private final SseConnectionManager connectionManager;

    public String generateTicket(Long userId) {
        return ticketService.generateTicket(userId);
    }

    public SseEmitter connect(String ticket) {
        Long userId = ticketService.validateAndConsumeTicket(ticket);
        if (userId == null) {
            log.warn("SSE 连接失败：无效的 Ticket");
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"message\":\"无效或过期的连接票据\"}"));
                emitter.complete();
            } catch (Exception ignored) {
            }
            return emitter;
        }
        log.info("SSE 连接建立: userId={}", userId);
        return connectionManager.createConnection(userId);
    }

    public void disconnect(Long userId) {
        connectionManager.removeConnection(userId);
    }

    public int getConnectionCount() {
        return connectionManager.getConnectionCount();
    }
}
