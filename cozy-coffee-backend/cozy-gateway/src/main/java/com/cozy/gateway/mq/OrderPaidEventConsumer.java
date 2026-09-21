package com.cozy.gateway.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderPaidEvent;
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
 * 支付事件消费者（Gateway 侧）：**清管理端缓存 + 广播 new_order 提醒商家**。
 * <p>
 * 这才是「商家可以开始做这一单了」的唯一信号。下单（ORDER_CREATED）只清缓存不提醒，
 * 因为未付款订单可能被弃单，提前提醒等于给店里制造噪声。
 * <p>
 * 用 BROADCASTING：SSE 长连接绑定在具体 Gateway 实例上，每个实例都必须收到事件，
 * 才能通知连在自己身上的那些管理端页面。
 * <p>
 * 消费失败仅打日志，不重试 —— 这是尽力而为的提醒：
 * 事件丢了不丢订单，管理端显式刷新走 noCache 回源，最坏也不过晚一个 TTL（30s）自愈。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_PAID,
        consumerGroup = "cozy-gateway-sse-paid",
        messageModel = MessageModel.BROADCASTING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class OrderPaidEventConsumer implements RocketMQListener<OrderPaidEvent> {

    private final SseEventPublisher sseEventPublisher;
    private final AdminOrderCacheEvictor cacheEvictor;

    @Override
    public void onMessage(OrderPaidEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        log.info("消费 order_paid 事件: orderId={}, orderNo={}", event.getOrderId(), event.getOrderNo());

        // 先清缓存再推送：管理端收到提醒后会立刻回源，若此时仍是旧缓存就白跑一趟
        try {
            cacheEvictor.evictAll();
        } catch (Exception e) {
            log.warn("异步清理管理端缓存失败: orderId={}", event.getOrderId(), e);
        }

        try {
            sseEventPublisher.publishNewOrderEnhanced(
                    event.getOrderId(),
                    event.getOrderNo(),
                    event.getUsername(),
                    event.getPayAmount(),
                    event.getItemCount());
        } catch (Exception e) {
            log.warn("异步推送新订单 SSE 失败: orderId={}", event.getOrderId(), e);
        }
    }
}
