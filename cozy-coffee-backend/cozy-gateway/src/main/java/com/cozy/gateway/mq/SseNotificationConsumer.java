package com.cozy.gateway.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.gateway.sse.SseEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单完成 → SSE 通知用户（BROADCASTING 模式，每个 Gateway 实例推自己的 SSE 长连接）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_COMPLETED,
        consumerGroup = "cozy-gateway-sse-completed",
        messageModel = MessageModel.BROADCASTING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class SseNotificationConsumer implements RocketMQListener<OrderCompletedEvent> {

    private final SseEventPublisher sseEventPublisher;

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (event.getUserId() == null) {
            return;
        }
        int points = event.getPointsEarned() != null ? event.getPointsEarned() : 0;
        int exp = event.getExpEarned() != null ? event.getExpEarned() : 0;
        try {
            sseEventPublisher.notifyOrderCompleted(
                    event.getUserId(),
                    event.getOrderId(),
                    points,
                    exp);
        } catch (Exception e) {
            log.warn("SSE 通知失败: userId={}, orderId={}", event.getUserId(), event.getOrderId(), e);
        }
    }
}
