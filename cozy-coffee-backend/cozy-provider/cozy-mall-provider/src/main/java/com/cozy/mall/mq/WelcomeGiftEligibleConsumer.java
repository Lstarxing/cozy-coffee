package com.cozy.mall.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.UserEventKeys;
import com.cozy.common.mq.WelcomeGiftEligibleEvent;
import com.cozy.mall.api.PointsMallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.USER_EVENTS,
        selectorExpression = MqTags.WELCOME_GIFT_ELIGIBLE,
        consumerGroup = "cozy-mall-welcome-gift",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY)
public class WelcomeGiftEligibleConsumer implements RocketMQListener<WelcomeGiftEligibleEvent> {

    private final PointsMallService pointsMallService;

    @Override
    public void onMessage(WelcomeGiftEligibleEvent event) {
        validate(event);
        try {
            // 必须复用新人券专用业务入口，不能走模板兜底的通用发券方法。
            pointsMallService.issueNewUserCoupon(event.getUserId());
            log.info("新人礼资格事件消费成功: userId={}, uniqueKey={}", event.getUserId(), event.getUniqueKey());
        } catch (Exception e) {
            throw new IllegalStateException("新人礼资格事件消费失败: " + event.getUniqueKey(), e);
        }
    }

    private void validate(WelcomeGiftEligibleEvent event) {
        if (event == null || event.getUserId() == null || event.getOccurredAt() == null
                || !UserEventKeys.welcomeGift(event.getUserId()).equals(event.getUniqueKey())) {
            throw new IllegalArgumentException("WELCOME_GIFT_ELIGIBLE 事件契约不合法");
        }
    }
}
