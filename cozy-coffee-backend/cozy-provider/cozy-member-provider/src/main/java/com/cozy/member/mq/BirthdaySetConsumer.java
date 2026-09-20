package com.cozy.member.mq;

import com.cozy.common.mq.BirthdaySetEvent;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
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
        selectorExpression = MqTags.BIRTHDAY_SET,
        consumerGroup = "cozy-member-birthday-set",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY)
public class BirthdaySetConsumer implements RocketMQListener<BirthdaySetEvent> {

    private final MemberService memberService;

    @Override
    public void onMessage(BirthdaySetEvent event) {
        validate(event);
        try {
            memberService.grantBirthdayReward(event.getUserId(), event.getBenefitYear());
            log.info("生日设置事件消费成功: userId={}, benefitYear={}, uniqueKey={}",
                    event.getUserId(), event.getBenefitYear(), event.getUniqueKey());
        } catch (Exception e) {
            throw new IllegalStateException("生日设置事件消费失败: " + event.getUniqueKey(), e);
        }
    }

    private void validate(BirthdaySetEvent event) {
        if (event == null || event.getUserId() == null || event.getBenefitYear() == null
                || event.getBenefitYear() <= 0 || event.getOccurredAt() == null
                || !UserEventKeys.birthday(event.getUserId(), event.getBenefitYear()).equals(event.getUniqueKey())) {
            throw new IllegalArgumentException("BIRTHDAY_SET 事件契约不合法");
        }
    }
}
