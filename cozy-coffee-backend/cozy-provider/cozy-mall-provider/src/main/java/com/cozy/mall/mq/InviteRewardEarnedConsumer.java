package com.cozy.mall.mq;

import com.cozy.common.constant.InviteRewardConfig;
import com.cozy.common.mq.InviteRewardEarnedEvent;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.UserEventKeys;
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
        selectorExpression = MqTags.INVITE_REWARD_EARNED,
        consumerGroup = "cozy-mall-invite-reward",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY)
public class InviteRewardEarnedConsumer implements RocketMQListener<InviteRewardEarnedEvent> {

    private final PointsMallService pointsMallService;
    private final InviteRewardConfig rewardConfig;

    @Override
    public void onMessage(InviteRewardEarnedEvent event) {
        validate(event);
        try {
            pointsMallService.issueCouponToUser(event.getInviterId(), rewardConfig.getCouponType(),
                    event.getUniqueKey(), rewardConfig.getMinAmount(), rewardConfig.getDiscountAmount(),
                    rewardConfig.getValidDays());
            log.info("邀请奖励事件消费成功: inviteeUserId={}, inviterId={}, uniqueKey={}",
                    event.getInviteeUserId(), event.getInviterId(), event.getUniqueKey());
        } catch (Exception e) {
            throw new IllegalStateException("邀请奖励事件消费失败: " + event.getUniqueKey(), e);
        }
    }

    private void validate(InviteRewardEarnedEvent event) {
        if (event == null || event.getInviteeUserId() == null || event.getInviterId() == null
                || event.getOccurredAt() == null
                || !UserEventKeys.inviteReward(event.getInviteeUserId(), event.getInviterId())
                        .equals(event.getUniqueKey())) {
            throw new IllegalArgumentException("INVITE_REWARD_EARNED 事件契约不合法");
        }
    }
}
