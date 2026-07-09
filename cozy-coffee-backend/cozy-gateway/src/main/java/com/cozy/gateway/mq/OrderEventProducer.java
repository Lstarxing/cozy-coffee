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
 * syncSend 等待 broker ACK，失败重试 3 次（指数退避 1s/2s/4s）。
 * order_completed 是积分/EXP/首单奖励/月度任务的触发器，事件丢失 = 用户下单完成拿不到积分。
 * C5 修复：sendOneWay -> syncSend，与 order_cancelled 的 Outbox 模式可靠性对等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    private static final int SEND_RETRY_MAX = 3;
    private static final long SEND_RETRY_BASE_DELAY_MS = 1000;

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
        for (int attempt = 1; attempt <= SEND_RETRY_MAX; attempt++) {
            try {
                rocketMQTemplate.syncSend(
                        destination,
                        MessageBuilder.withPayload(payload)
                                .setHeader("KEYS", String.valueOf(orderId))
                                .build());
                log.debug("MQ 派发 {}: orderId={}, attempt={}", tag, orderId, attempt);
                return;
            } catch (Exception e) {
                if (attempt == SEND_RETRY_MAX) {
                    log.error("MQ 派发 {} 最终失败 ({}次重试): orderId={}", tag, SEND_RETRY_MAX, orderId, e);
                } else {
                    long delay = SEND_RETRY_BASE_DELAY_MS * (1L << (attempt - 1));
                    log.warn("MQ 派发 {} 失败, attempt={}/{}, {}ms 后重试: orderId={}",
                            tag, attempt, SEND_RETRY_MAX, delay, orderId);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}
