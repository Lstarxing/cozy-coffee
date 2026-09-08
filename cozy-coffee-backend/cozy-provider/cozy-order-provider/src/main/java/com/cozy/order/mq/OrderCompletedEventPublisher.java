package com.cozy.order.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.order.entity.ShopOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ORDER_COMPLETED 统一发布器（Outbox 模式）：
 * - 仅在「确认发奖 CAS 赢家」时调用，保证单订单只发一次事件；
 * - 事件在调用方事务内写 message_outbox(PENDING)，broker 不可用也不丢，
 *   OutboxService 兜底任务恢复后自动重投，无需人工补发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventPublisher {

    private final OutboxService outboxService;

    public void publish(ShopOrder order, int expEarned, int pointsEarned,
                        boolean isFirstOrder, boolean hasNewProduct, boolean isDelivery) {
        if (order == null || order.getId() == null) {
            return;
        }
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .payAmount(order.getPayAmount())
                .expEarned(expEarned)
                .pointsEarned(pointsEarned)
                .isFirstOrder(isFirstOrder)
                .hasNewProduct(hasNewProduct)
                .isDelivery(isDelivery)
                .occurredAt(LocalDateTime.now())
                .build();

        outboxService.publish(
                MqTopics.ORDER_EVENTS,
                MqTags.ORDER_COMPLETED,
                "order_completed",
                order.getId(),
                event);
        log.info("ORDER_COMPLETED 已写入 outbox: orderId={}, exp={}, points={}",
                order.getId(), expEarned, pointsEarned);
    }
}
