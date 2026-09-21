package com.cozy.gateway.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCreatedEvent;
import com.cozy.gateway.cache.AdminOrderCacheEvictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 下单事件消费者（Gateway 侧）：**只清管理端缓存，不提醒商家**。
 * <p>
 * 为什么不再推 new_order：未付款的订单不该触发商家开始制作 —— 否则弃单会制造噪声。
 * 「可以开始做这一单了」的信号是 {@link OrderPaidEventConsumer}（来自 ORDER_PAID）。
 * <p>
 * 用 BROADCASTING：管理端缓存虽在 Redis（其实清一次就够），但每个 Gateway 实例还可能有
 * 本地缓存与本地状态，广播给所有实例更稳；清缓存本身幂等，多清一次无害。
 * <p>
 * 消费失败仅打日志，不重试 —— 缓存最多一个 TTL（30s）后自愈。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_CREATED,
        consumerGroup = "cozy-gateway-order-cache",
        messageModel = MessageModel.BROADCASTING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class OrderCreatedEventConsumer implements RocketMQListener<OrderCreatedEvent> {

    private final AdminOrderCacheEvictor cacheEvictor;

    @Override
    public void onMessage(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        log.info("消费 order_created 事件（仅清缓存，不提醒商家）: orderId={}, orderNo={}",
                event.getOrderId(), event.getOrderNo());

        try {
            cacheEvictor.evictAll();
        } catch (Exception e) {
            log.warn("异步清理管理端缓存失败: orderId={}", event.getOrderId(), e);
        }
    }
}
