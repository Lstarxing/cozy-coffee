package com.cozy.gateway.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器
 * 管理所有管理员的 SSE 连接
 */
@Slf4j
@Service
public class SseConnectionManager {

    // 连接存储：userId -> SseEmitter
    private final Map<Long, SseEmitter> connections = new ConcurrentHashMap<>();

    // SSE 连接超时时间（30分钟）
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 创建 SSE 连接
     */
    public SseEmitter createConnection(Long userId) {
        // 先移除旧连接
        removeConnection(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 连接完成回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: userId={}", userId);
            connections.remove(userId, emitter);
        });

        // 连接超时回调
        emitter.onTimeout(() -> {
            log.info("SSE 连接超时: userId={}", userId);
            connections.remove(userId, emitter);
        });

        // 连接错误回调
        emitter.onError((e) -> {
            log.warn("SSE 连接错误: userId={}, error={}", userId, e.getMessage());
            connections.remove(userId, emitter);
        });

        connections.put(userId, emitter);
        log.info("SSE 连接建立: userId={}, 当前连接数={}", userId, connections.size());

        // 发送连接成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE连接成功\"}"));
        } catch (IOException e) {
            log.error("发送连接成功事件失败", e);
        }

        return emitter;
    }

    /**
     * 移除连接
     */
    public void removeConnection(Long userId) {
        SseEmitter emitter = connections.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // 忽略
            }
            log.info("SSE 连接移除: userId={}", userId);
        }
    }

    /**
     * 广播事件到所有管理员
     */
    public void broadcast(String eventName, Object data) {
        log.info("SSE 广播事件: event={}, 准备发送给 {} 个连接", eventName, connections.size());
        connections.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.debug("SSE 广播成功: event={}, targetUserId={}", eventName, userId);
            } catch (IOException e) {
                log.warn("SSE 广播失败: event={}, userId={}, error={}", eventName, userId, e.getMessage());
                connections.remove(userId);
            }
        });
    }

    /**
     * 向特定用户发送事件
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = connections.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.info("SSE 发送给用户: userId={}, event={}", userId, eventName);
            } catch (IOException e) {
                log.warn("SSE 发送给用户失败: userId={}, error={}", userId, e.getMessage());
                connections.remove(userId);
            }
        } else {
            log.debug("用户未连接 SSE: userId={}", userId);
        }
    }

    /**
     * 发送心跳保活
     */
    @Scheduled(fixedRate = 15000) // 每15秒发送一次心跳
    public void sendHeartbeat() {
        if (connections.isEmpty()) {
            return;
        }
        log.debug("SSE 发送心跳: 连接数={}", connections.size());
        connections.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("{\"time\":" + System.currentTimeMillis() + "}"));
            } catch (IOException e) {
                log.warn("SSE 心跳发送失败: userId={}", userId);
                connections.remove(userId);
            }
        });
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return connections.size();
    }
}
