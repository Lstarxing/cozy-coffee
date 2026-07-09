package com.cozy.member.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.member.api.MemberService;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.mapper.PointsLotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 订单完成 → 发放积分（CLUSTERING 模式，幂等：检查 PointsLot 是否已存在）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_COMPLETED,
        consumerGroup = "cozy-member-point",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class PointConsumer implements RocketMQListener<OrderCompletedEvent> {

    private final MemberService memberService;
    private final PointsLotMapper pointsLotMapper;

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (event.getPointsEarned() == null || event.getPointsEarned() <= 0) {
            return;
        }
        // 幂等：检查是否已发放
        long count = pointsLotMapper.selectCount(new LambdaQueryWrapper<PointsLot>()
                .eq(PointsLot::getSourceType, "order_completed")
                .eq(PointsLot::getSourceId, event.getOrderId()));
        if (count > 0) {
            log.debug("积分已发放，跳过: orderId={}", event.getOrderId());
            return;
        }
        try {
            memberService.addPointsWithLot(event.getUserId(), event.getPointsEarned(),
                    "order_completed", event.getOrderId(),
                    "咖啡订单完成: " + event.getOrderNo());
        } catch (DuplicateKeyException e) {
            log.info("积分并发发放被拦截(幂等): orderId={}", event.getOrderId());
            return;
        }
        log.info("积分发放成功: orderId={}, points={}", event.getOrderId(), event.getPointsEarned());
    }
}
