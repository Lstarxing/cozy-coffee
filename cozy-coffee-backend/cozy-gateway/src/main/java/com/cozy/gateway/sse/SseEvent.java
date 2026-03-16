package com.cozy.gateway.sse;

import lombok.Data;
import java.io.Serializable;

/**
 * SSE 事件数据
 */
@Data
public class SseEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 事件类型 */
    private String type;

    /** 事件消息 */
    private String message;

    /** 关联 ID（如订单ID） */
    private Long entityId;

    /** 额外数据 */
    private Object data;

    /** 时间戳 */
    private long timestamp;

    public SseEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public SseEvent(String type, String message) {
        this();
        this.type = type;
        this.message = message;
    }

    public SseEvent(String type, String message, Long entityId) {
        this(type, message);
        this.entityId = entityId;
    }

    // 预定义事件类型
    public static final String TYPE_NEW_ORDER = "new_order";
    public static final String TYPE_NEW_REDEMPTION = "new_redemption";
    public static final String TYPE_ORDER_CANCELLED = "order_cancelled";

    /**
     * 新订单事件
     */
    public static SseEvent newOrder(Long orderId, String orderNo) {
        SseEvent event = new SseEvent(TYPE_NEW_ORDER, "有新的咖啡订单", orderId);
        event.setData(orderNo);
        return event;
    }

    /**
     * 新订单事件（增强版，包含更多信息）
     */
    public static SseEvent newOrderEnhanced(Long orderId, String orderNo, String username,
            java.math.BigDecimal totalAmount, Integer itemCount) {
        SseEvent event = new SseEvent(TYPE_NEW_ORDER,
                String.format("新订单：%s 下单 ¥%.2f", username, totalAmount), orderId);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("orderNo", orderNo);
        data.put("username", username);
        data.put("totalAmount", totalAmount);
        data.put("itemCount", itemCount);
        event.setData(data);
        return event;
    }

    /**
     * 新兑换订单事件
     */
    public static SseEvent newRedemption(Long orderId) {
        return new SseEvent(TYPE_NEW_REDEMPTION, "有新的积分兑换订单", orderId);
    }
}
