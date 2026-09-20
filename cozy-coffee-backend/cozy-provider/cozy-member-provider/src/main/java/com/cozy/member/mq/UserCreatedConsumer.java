package com.cozy.member.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.UserCreatedEvent;
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
        selectorExpression = MqTags.USER_CREATED,
        consumerGroup = "cozy-member-user-created",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY)
public class UserCreatedConsumer implements RocketMQListener<UserCreatedEvent> {

    private final MemberService memberService;

    @Override
    public void onMessage(UserCreatedEvent event) {
        validate(event);
        try {
            memberService.createMember(event.getUserId());
            log.info("用户建档事件消费成功: userId={}, uniqueKey={}", event.getUserId(), event.getUniqueKey());
        } catch (Exception e) {
            throw new IllegalStateException("用户建档事件消费失败: " + event.getUniqueKey(), e);
        }
    }

    private void validate(UserCreatedEvent event) {
        if (event == null || event.getUserId() == null || event.getOccurredAt() == null
                || !UserEventKeys.userCreated(event.getUserId()).equals(event.getUniqueKey())) {
            throw new IllegalArgumentException("USER_CREATED 事件契约不合法");
        }
    }
}
