package com.cozy.mall.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCancelledEvent;
import com.cozy.mall.api.PointsMallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单取消 -> 回滚优惠券（CLUSTERING 模式）
 * <p>
 * 幂等保障：PointsMallService.rollbackCoupons 在同一事务内先写 coupon_rollback_inbox，
 * 同一回滚事件重复投递不会再次修改券状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_CANCELLED,
        consumerGroup = "cozy-mall-coupon-rollback",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class CouponRollbackConsumer implements RocketMQListener<OrderCancelledEvent> {

    private final PointsMallService pointsMallService;

    @Override
    public void onMessage(OrderCancelledEvent event) {
        log.info("消费 order_cancelled 事件: orderId={}, userId={}, mainCoupon={}, addonCount={}",
                event.getOrderId(), event.getUserId(), event.getAppliedCouponId(),
                event.getAddonCouponIds() == null ? 0 : event.getAddonCouponIds().size());

        try {
            String eventId = event.getRollbackEventId();
            if (eventId == null || eventId.isBlank()) {
                // 兼容升级前已经在 MQ 中的旧事件；orderId 非空时仍可稳定去重。
                eventId = event.getOrderId() == null ? null : "legacy-order:" + event.getOrderId();
            }
            if (eventId == null) {
                throw new IllegalArgumentException("券回滚事件缺少幂等键");
            }
            pointsMallService.rollbackCoupons(eventId, event.getOrderId(), event.getUserId(),
                    event.getAppliedCouponId(), event.getAddonCouponIds());
            log.info("优惠券整组回滚成功: eventId={}, orderId={}", eventId, event.getOrderId());
        } catch (Exception e) {
            log.error("优惠券整组回滚失败: eventId={}, orderId={}, error={}",
                    event.getRollbackEventId(), event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("券回滚失败: " + event.getRollbackEventId(), e);
        }
    }
}
