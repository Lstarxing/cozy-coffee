package com.cozy.order.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.order.entity.ShopOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ORDER_COMPLETED 统一发布器：仅在「确认发奖 CAS 赢家」时调用，保证单订单只发一次事件。
 * 原外送 Job 内联的发布逻辑收敛到这里，grantRewards 与兜底 Job 共用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventPublisher {

    private static final int SEND_RETRY_MAX = 3;
    private static final long SEND_RETRY_BASE_DELAY_MS = 1000;

    private final RocketMQTemplate rocketMQTemplate;

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

        String destination = MqTopics.ORDER_EVENTS + ":" + MqTags.ORDER_COMPLETED;
        for (int attempt = 1; attempt <= SEND_RETRY_MAX; attempt++) {
            try {
                rocketMQTemplate.syncSend(
                        destination,
                        MessageBuilder.withPayload(event)
                                .setHeader("KEYS", String.valueOf(order.getId()))
                                .build());
                return;
            } catch (Exception e) {
                if (attempt == SEND_RETRY_MAX) {
                    log.error("派发 ORDER_COMPLETED 最终失败: orderId={}", order.getId(), e);
                } else {
                    long delay = SEND_RETRY_BASE_DELAY_MS * (1L << (attempt - 1));
                    log.warn("派发 ORDER_COMPLETED 失败, attempt={}/{}, {}ms 后重试: orderId={}",
                            attempt, SEND_RETRY_MAX, delay, order.getId());
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
