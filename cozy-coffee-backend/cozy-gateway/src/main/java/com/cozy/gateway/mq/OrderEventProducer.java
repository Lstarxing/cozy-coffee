package com.cozy.gateway.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单事件生产者。
 * 使用 sendOneWay 保证下单主流程不因 MQ 抖动而阻塞：
 * - 不等待 broker 确认
 * - 失败仅记日志，事件会丢，但订单本身已落库
 * 对可靠性要求更高的事件（如积分发放）应改用 sendSync + 本地事务表兜底。
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
        String destination = MqTopics.ORDER_EVENTS + ":" + MqTags.ORDER_CREATED;
        try {
            rocketMQTemplate.sendOneWay(
                    destination,
                    MessageBuilder.withPayload(event)
                            .setHeader("KEYS", String.valueOf(event.getOrderId()))
                            .build());
            log.debug("MQ 派发 order_created: orderId={}, orderNo={}", event.getOrderId(), event.getOrderNo());
        } catch (Exception e) {
            log.warn("MQ 派发 order_created 失败，降级为丢弃: orderId={}, orderNo={}",
                    event.getOrderId(), event.getOrderNo(), e);
        }
    }
}
