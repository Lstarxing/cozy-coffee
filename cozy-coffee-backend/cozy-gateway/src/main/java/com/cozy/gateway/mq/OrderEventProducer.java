package com.cozy.gateway.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.common.mq.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单事件生产者。
 * sendOneWay 不等待 broker ACK，保证主流程不被 MQ 抖动阻塞。
 * 失败仅记日志，事件丢失但订单本身已落库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        send(MqTags.ORDER_CREATED, event, event.getOrderId());
    }

    public void publishOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        send(MqTags.ORDER_COMPLETED, event, event.getOrderId());
    }

    private void send(String tag, Object payload, Long orderId) {
        String destination = MqTopics.ORDER_EVENTS + ":" + tag;
        try {
            rocketMQTemplate.sendOneWay(
                    destination,
                    MessageBuilder.withPayload(payload)
                            .setHeader("KEYS", String.valueOf(orderId))
                            .build());
            log.debug("MQ 派发 {}: orderId={}", tag, orderId);
        } catch (Exception e) {
            log.warn("MQ 派发 {} 失败，降级为丢弃: orderId={}", tag, orderId, e);
        }
    }
}
