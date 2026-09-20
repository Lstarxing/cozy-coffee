package com.cozy.member.mq;

import com.cozy.common.constant.ProfileRewardConfig;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.ProfileCompletedEvent;
import com.cozy.common.mq.UserEventKeys;
import com.cozy.member.api.MemberService;
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
        selectorExpression = MqTags.PROFILE_COMPLETED,
        consumerGroup = "cozy-member-profile-completed",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY)
public class ProfileCompletedConsumer implements RocketMQListener<ProfileCompletedEvent> {

    private final MemberService memberService;
    private final ProfileRewardConfig rewardConfig;

    @Override
    public void onMessage(ProfileCompletedEvent event) {
        validate(event);
        try {
            memberService.addPointsWithLot(event.getUserId(), rewardConfig.getPoints(), rewardConfig.getSourceType(),
                    event.getUserId(), rewardConfig.getDescription());
            log.info("完善资料事件消费成功: userId={}, uniqueKey={}", event.getUserId(), event.getUniqueKey());
        } catch (Exception e) {
            throw new IllegalStateException("完善资料事件消费失败: " + event.getUniqueKey(), e);
        }
    }

    private void validate(ProfileCompletedEvent event) {
        if (event == null || event.getUserId() == null || event.getOccurredAt() == null
                || !UserEventKeys.profileCompleted(event.getUserId()).equals(event.getUniqueKey())) {
            throw new IllegalArgumentException("PROFILE_COMPLETED 事件契约不合法");
        }
    }
}
