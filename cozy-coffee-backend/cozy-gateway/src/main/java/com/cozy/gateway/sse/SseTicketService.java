package com.cozy.gateway.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Ticket 服务
 * 用于生成和验证一次性连接票据
 */
@Slf4j
@Service
public class SseTicketService {

    // 票据存储：ticket -> userId
    private final Map<String, TicketInfo> tickets = new ConcurrentHashMap<>();

    // 票据有效期（秒）
    private static final int TICKET_EXPIRE_SECONDS = 30;

    /**
     * 生成票据
     */
    public String generateTicket(Long userId) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(TICKET_EXPIRE_SECONDS);
        tickets.put(ticket, new TicketInfo(userId, expireAt));
        log.debug("生成 SSE Ticket: userId={}, ticket={}", userId, ticket);
        return ticket;
    }

    /**
     * 验证并消费票据（一次性）
     * 
     * @return userId 如果有效，null 如果无效或过期
     */
    public Long validateAndConsumeTicket(String ticket) {
        if (ticket == null || ticket.isEmpty()) {
            return null;
        }
        TicketInfo info = tickets.remove(ticket);
        if (info == null) {
            log.warn("SSE Ticket 不存在或已使用: {}", ticket);
            return null;
        }
        if (LocalDateTime.now().isAfter(info.expireAt)) {
            log.warn("SSE Ticket 已过期: {}", ticket);
            return null;
        }
        log.debug("SSE Ticket 验证成功: userId={}", info.userId);
        return info.userId;
    }

    /**
     * 定时清理过期票据
     */
    @Scheduled(fixedRate = 60000) // 每分钟清理一次
    public void cleanExpiredTickets() {
        LocalDateTime now = LocalDateTime.now();
        int before = tickets.size();
        tickets.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expireAt));
        int removed = before - tickets.size();
        if (removed > 0) {
            log.debug("清理过期 SSE Ticket: {} 个", removed);
        }
    }

    /**
     * 票据信息
     */
    private static class TicketInfo {
        final Long userId;
        final LocalDateTime expireAt;

        TicketInfo(Long userId, LocalDateTime expireAt) {
            this.userId = userId;
            this.expireAt = expireAt;
        }
    }
}
