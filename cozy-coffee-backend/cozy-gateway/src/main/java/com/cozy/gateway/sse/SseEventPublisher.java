package com.cozy.gateway.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SSE 事件发布服务
 * 用于在业务代码中发布事件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseEventPublisher {

    private final SseConnectionManager connectionManager;

    /**
     * 广播新订单事件
     */
    public void publishNewOrder(Long orderId, String orderNo) {
        log.info("发布新订单事件: orderId={}, orderNo={}, 当前连接数={}",
                orderId, orderNo, connectionManager.getConnectionCount());
        SseEvent event = SseEvent.newOrder(orderId, orderNo);
        connectionManager.broadcast("new_order", event);
    }

    /**
     * 广播新订单事件（增强版）
     */
    public void publishNewOrderEnhanced(Long orderId, String orderNo, String username,
            java.math.BigDecimal totalAmount, Integer itemCount) {
        log.info("发布新订单事件(增强): orderId={}, orderNo={}, user={}, amount={}",
                orderId, orderNo, username, totalAmount);
        SseEvent event = SseEvent.newOrderEnhanced(orderId, orderNo, username, totalAmount, itemCount);
        connectionManager.broadcast("new_order", event);
    }

    /**
     * 广播新兑换订单事件
     */
    public void publishNewRedemption(Long orderId) {
        log.info("发布新兑换订单事件: orderId={}", orderId);
        SseEvent event = SseEvent.newRedemption(orderId);
        connectionManager.broadcast("new_redemption", event);
    }

    /**
     * 广播自定义事件
     */
    public void publish(String eventName, Object data) {
        connectionManager.broadcast(eventName, data);
    }

    /**
     * 通知用户订单已完成（用于触发前端刷新）
     */
    public void notifyOrderCompleted(Long userId, Long orderId, int pointsEarned, int expEarned) {
        log.info("通知用户订单完成: userId={}, orderId={}, points={}, exp={}",
                userId, orderId, pointsEarned, expEarned);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("orderId", orderId);
        data.put("pointsEarned", pointsEarned);
        data.put("expEarned", expEarned);
        data.put("message", "您的订单已完成，获得 " + pointsEarned + " 积分");
        connectionManager.sendToUser(userId, "order_completed", data);
    }
}
