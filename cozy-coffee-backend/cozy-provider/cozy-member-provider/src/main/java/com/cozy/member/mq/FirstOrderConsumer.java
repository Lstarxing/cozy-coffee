package com.cozy.member.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.member.api.MemberService;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.mapper.PointsLotMapper;
import com.cozy.user.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单完成 → 首单奖励（+200 积分）+ 邀请人奖励
 * CLUSTERING 模式，幂等检查 PointsLot.sourceType='first_order_bonus'
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopics.ORDER_EVENTS,
        selectorExpression = MqTags.ORDER_COMPLETED,
        consumerGroup = "cozy-member-first-order",
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class FirstOrderConsumer implements RocketMQListener<OrderCompletedEvent> {

    private final MemberService memberService;
    private final PointsLotMapper pointsLotMapper;

    @DubboReference(check = false)
    private UserService userService;

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (!Boolean.TRUE.equals(event.getIsFirstOrder())) {
            return;
        }
        long count = pointsLotMapper.selectCount(new LambdaQueryWrapper<PointsLot>()
                .eq(PointsLot::getSourceType, "first_order_bonus")
                .eq(PointsLot::getSourceId, event.getOrderId()));
        if (count > 0) {
            log.debug("首单奖励已发放，跳过: orderId={}", event.getOrderId());
            return;
        }
        memberService.addPointsWithLot(event.getUserId(), 200, "first_order_bonus",
                event.getOrderId(), "新用户首单奖励");
        log.info("首单奖励发放成功: userId={}, orderId={}", event.getUserId(), event.getOrderId());

        try {
            if (userService != null) {
                boolean granted = userService.grantInviteRewardOnFirstOrder(event.getUserId());
                if (granted) {
                    log.info("首单邀请奖励发放成功: userId={}", event.getUserId());
                }
            }
        } catch (Exception ex) {
            log.warn("首单邀请奖励发放失败: userId={}, error={}", event.getUserId(), ex.getMessage());
        }
    }
}
