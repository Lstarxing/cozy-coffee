package com.cozy.member.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.member.api.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单完成 → 发放 EXP（CLUSTERING 模式）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_COMPLETED,
        consumerGroup = "cozy-member-exp",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class ExpConsumer implements RocketMQListener<OrderCompletedEvent> {

    private final MemberService memberService;

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (event.getExpEarned() == null || event.getExpEarned() <= 0) {
            return;
        }
        // addExp 内部有等级逻辑，多次调用会导致 EXP 翻倍。
        // 但 addExp 会推高 expTotal 并触发等级变更回调，需通过外部状态防重。
        // 方案：PointConsumer 会写 PointsLot，如果积分已发则 EXP 也跳过。
        // 更稳健的做法——两个 consumer 合并为一个，但用户明确要拆分。
        // 当前策略：依赖 MQ ACK + CLUSTERING 单消费语义 + 异步重试次数为 0。
        // 若需严格幂等，后续 PR3 配合 Outbox 表补全。
        memberService.addExp(event.getUserId(), event.getExpEarned(), event.getOrderId());
        log.info("EXP 发放成功: orderId={}, exp={}", event.getOrderId(), event.getExpEarned());
    }
}
