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

import java.time.LocalDateTime;

/**
 * 订单完成 → 发放 EXP（CLUSTERING 模式，幂等：PointsLot.sourceType='order_exp' 防重）
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
    private final PointsLotMapper pointsLotMapper;

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (event.getExpEarned() == null || event.getExpEarned() <= 0) {
            return;
        }

        // 幂等：PointsLot.sourceType='order_exp' 作为防重标记
        long count = pointsLotMapper.selectCount(new LambdaQueryWrapper<PointsLot>()
                .eq(PointsLot::getSourceType, "order_exp")
                .eq(PointsLot::getSourceId, event.getOrderId()));
        if (count > 0) {
            log.debug("EXP 已发放，跳过: orderId={}", event.getOrderId());
            return;
        }

        memberService.addExp(event.getUserId(), event.getExpEarned(), event.getOrderId());

        // 写防重标记（与 addExp 在同一本地事务外 -- memberService 内部有 @Transactional）
        try {
            PointsLot lot = new PointsLot();
            lot.setUserId(event.getUserId());
            lot.setInitialAmount(0);
            lot.setRemaining(0);
            lot.setSourceType("order_exp");
            lot.setSourceId(event.getOrderId());
            lot.setExpiresAt(LocalDateTime.now().plusYears(1));
            lot.setCreatedAt(LocalDateTime.now());
            pointsLotMapper.insert(lot);
        } catch (DuplicateKeyException e) {
            log.info("EXP 防重标记并发插入被拦截: orderId={}", event.getOrderId());
        }

        log.info("EXP 发放成功: orderId={}, exp={}", event.getOrderId(), event.getExpEarned());
    }
}
