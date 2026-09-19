package com.cozy.mall.mq;

import com.cozy.common.mq.CouponGrantRequestedEvent;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.mall.api.PointsMallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费会员域的发券请求（member 经本地 outbox → MQ 投递，取代原先的同步 RPC 调用）。
 *
 * <p>幂等：{@code issueCouponToUser} 内部按 {@code user_coupon.coupon_code}（= uniqueKey）去重，
 * 重复投递不会重复发券。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.MEMBER_EVENTS,
        selectorExpression = MqTags.COUPON_GRANT_REQUESTED,
        consumerGroup = "cozy-mall-coupon-grant",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class CouponGrantConsumer implements RocketMQListener<CouponGrantRequestedEvent> {

    private final PointsMallService pointsMallService;

    @Override
    public void onMessage(CouponGrantRequestedEvent event) {
        if (event.getUserId() == null || event.getUniqueKey() == null || event.getUniqueKey().isBlank()) {
            throw new IllegalArgumentException("发券请求缺少 userId 或 uniqueKey");
        }
        log.info("消费发券请求: userId={}, source={}, uniqueKey={}",
                event.getUserId(), event.getSource(), event.getUniqueKey());
        try {
            pointsMallService.issueCouponToUser(event.getUserId(), event.getCouponType(), event.getUniqueKey(),
                    event.getMinAmount(), event.getDiscountAmount(), event.getValidDays());
        } catch (Exception e) {
            log.error("发券失败: userId={}, uniqueKey={}, error={}",
                    event.getUserId(), event.getUniqueKey(), e.getMessage(), e);
            // 抛出 → RocketMQ 重投；配合消费端幂等，重复消费是安全的
            throw new IllegalStateException("发券失败: " + event.getUniqueKey(), e);
        }
    }
}
