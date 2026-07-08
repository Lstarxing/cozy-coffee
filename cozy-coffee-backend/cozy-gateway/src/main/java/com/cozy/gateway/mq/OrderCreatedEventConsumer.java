package com.cozy.gateway.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCreatedEvent;
import com.cozy.gateway.cache.AdminOrderCacheEvictor;
import com.cozy.gateway.sse.SseEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单事件消费者（Gateway 侧）。
 * <p>
 * 使用 BROADCASTING 广播模式：SSE 长连接绑定在具体 Gateway 实例上，
 * 每个实例必须都收到事件才能推送自己的客户端。
 * <p>
 * 消费失败仅打日志，不重试 —— SSE 是尽力推送，管理端缓存下次访问也会自愈。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_CREATED,
        consumerGroup = "cozy-gateway-sse",
        messageModel = MessageModel.BROADCASTING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class OrderCreatedEventConsumer implements RocketMQListener<OrderCreatedEvent> {

    private final SseEventPublisher sseEventPublisher;
    private final AdminOrderCacheEvictor cacheEvictor;

    @Override
    public void onMessage(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        log.info("消费 order_created 事件: orderId={}, orderNo={}", event.getOrderId(), event.getOrderNo());

        try {
            cacheEvictor.evictAll();
        } catch (Exception e) {
            log.warn("异步清理管理端缓存失败: orderId={}", event.getOrderId(), e);
        }

        try {
            if (event.getUsername() != null || event.getPayAmount() != null || event.getItemCount() != null) {
                sseEventPublisher.publishNewOrderEnhanced(
                        event.getOrderId(),
                        event.getOrderNo(),
                        event.getUsername(),
                        event.getPayAmount(),
                        event.getItemCount());
            } else {
                sseEventPublisher.publishNewOrder(event.getOrderId(), event.getOrderNo());
            }
        } catch (Exception e) {
            log.warn("异步推送新订单 SSE 失败: orderId={}", event.getOrderId(), e);
        }
    }
}
