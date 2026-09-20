package com.cozy.user.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.user.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单完成 → 认领邀请奖励资格（ADR 0001 §8 第 7 步）。
 *
 * <p>此前这一步是**反向**的：member 的 `FirstOrderConsumer` 通过 Dubbo 回调
 * `userService.grantInviteRewardOnFirstOrder`，构成 `member → user` 依赖。现在 user 侧
 * 自己消费 `ORDER_COMPLETED`，那条中间跳被删掉 —— 「首单奖励」留在 member（属它的域），
 * 「邀请资格认领」回到 user（属它的域），各管各的。
 *
 * <p><b>不吞异常</b>：认领失败要抛回 RocketMQ 重投。首单只有一次，吞掉就等于用户永久拿不到邀请券。
 * 幂等由 `grantInviteRewardOnFirstOrder` 内部的条件更新保证（ADR C4），重投安全。
 *
 * <p>只处理 `isFirstOrder=true` 的事件，与原来的回调条件一致（邀请奖励绑在**首单**上）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_COMPLETED,
        consumerGroup = "cozy-user-invite-reward",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class OrderCompletedInviteConsumer implements RocketMQListener<OrderCompletedEvent> {

    private final UserService userService;

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (!Boolean.TRUE.equals(event.getIsFirstOrder())) {
            return;
        }
        if (userService.grantInviteRewardOnFirstOrder(event.getUserId())) {
            log.info("首单邀请奖励资格已认领并入队: userId={}, orderId={}", event.getUserId(), event.getOrderId());
        }
    }
}
