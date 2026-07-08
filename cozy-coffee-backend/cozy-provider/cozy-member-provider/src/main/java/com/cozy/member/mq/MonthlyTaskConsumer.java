package com.cozy.member.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.member.api.MonthlyTaskService;
import com.cozy.member.entity.MonthlyTaskOrder;
import com.cozy.member.mapper.MonthlyTaskOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单完成 → 月度任务更新
 * CLUSTERING 模式，幂等检查 MonthlyTaskOrder 是否已存在
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_COMPLETED,
        consumerGroup = "cozy-member-monthly-task",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class MonthlyTaskConsumer implements RocketMQListener<OrderCompletedEvent> {

    private final MonthlyTaskService monthlyTaskService;
    private final MonthlyTaskOrderMapper monthlyTaskOrderMapper;

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (event.getPayAmount() == null || event.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // 幂等
        if (event.getOrderId() != null && monthlyTaskOrderMapper.selectCount(
                new LambdaQueryWrapper<MonthlyTaskOrder>()
                        .eq(MonthlyTaskOrder::getOrderId, event.getOrderId())) > 0) {
            log.debug("月度任务已更新，跳过: orderId={}", event.getOrderId());
            return;
        }
        monthlyTaskService.updateMonthlySpentWithDetails(
                event.getUserId(),
                event.getOrderId(),
                event.getPayAmount(),
                Boolean.TRUE.equals(event.getIsDelivery()),
                Boolean.TRUE.equals(event.getHasNewProduct()));
        log.info("月度任务更新成功: userId={}, orderId={}, amount={}, isDelivery={}, hasNew={}",
                event.getUserId(), event.getOrderId(), event.getPayAmount(),
                event.getIsDelivery(), event.getHasNewProduct());
    }
}
