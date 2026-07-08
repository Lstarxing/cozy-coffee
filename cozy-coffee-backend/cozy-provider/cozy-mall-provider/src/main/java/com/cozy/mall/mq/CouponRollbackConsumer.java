package com.cozy.mall.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCancelledEvent;
import com.cozy.member.api.PointsMallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单取消 -> 回滚优惠券（CLUSTERING 模式）
 * <p>
 * 幂等保障：PointsMallService.rollbackCoupon 内部已检查 status=USED 才回滚，
 * 多次消费同一事件不会重复回滚。
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

        if (event.getAppliedCouponId() != null) {
            rollbackSafely(event.getOrderId(), event.getAppliedCouponId(), event.getUserId());
        }

        List<Long> addonCouponIds = event.getAddonCouponIds();
        if (addonCouponIds != null && !addonCouponIds.isEmpty()) {
            for (Long id : addonCouponIds) {
                rollbackSafely(event.getOrderId(), id, event.getUserId());
            }
        }
    }

    private void rollbackSafely(Long orderId, Long couponId, Long userId) {
        try {
            pointsMallService.rollbackCoupon(couponId, userId);
            log.info("优惠券回滚成功: orderId={}, couponId={}", orderId, couponId);
        } catch (Exception e) {
            // rollbackCoupon 内部对非 USED 状态视为无需回滚并直接返回，不会抛异常；
            // 真正异常时抛出 -> RocketMQ 自动重试，达到死信阈值后进 DLQ
            log.error("优惠券回滚失败: orderId={}, couponId={}, error={}", orderId, couponId, e.getMessage(), e);
            throw new RuntimeException("券回滚失败: " + couponId, e);
        }
    }
}
