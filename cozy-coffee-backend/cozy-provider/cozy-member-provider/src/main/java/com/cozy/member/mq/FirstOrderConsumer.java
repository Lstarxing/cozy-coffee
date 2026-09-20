package com.cozy.member.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.FirstOrderRewardConfig;
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
import org.springframework.dao.DuplicateKeyException;
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

    // 首单奖励配置（单一事实源 @ConfigurationProperties，见 cozy.member.first-order）
    private final FirstOrderRewardConfig firstOrderRewardConfig;

    @DubboReference(check = false)
    private UserService userService;

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (!Boolean.TRUE.equals(event.getIsFirstOrder())) {
            return;
        }

        // 首单积分：靠 points_lots 的 (user_id, source_type, source_id) 幂等。
        // 关键：积分与邀请奖励是**两件独立的事** —— 这里只跳过"积分写入"，不能连邀请奖励一起跳过。
        // 否则"积分成功 + 邀请侧失败"之后重投时，会被这里提前 return 截断，邀请券永久丢失。
        long count = pointsLotMapper.selectCount(new LambdaQueryWrapper<PointsLot>()
                .eq(PointsLot::getSourceType, firstOrderRewardConfig.getSourceType())
                .eq(PointsLot::getSourceId, event.getOrderId()));
        if (count > 0) {
            log.debug("首单奖励已发放，跳过积分写入: orderId={}", event.getOrderId());
        } else {
            try {
                memberService.addPointsWithLot(event.getUserId(), firstOrderRewardConfig.getPoints(),
                        firstOrderRewardConfig.getSourceType(),
                        event.getOrderId(), "新用户首单奖励");
                log.info("首单奖励发放成功: userId={}, orderId={}", event.getUserId(), event.getOrderId());
            } catch (DuplicateKeyException e) {
                // 并发下的幂等冲突同样只影响积分，继续走邀请奖励
                log.info("首单奖励并发发放被拦截(幂等): orderId={}", event.getOrderId());
            }
        }

        // 邀请奖励：不吞异常 —— 抛出去让 RocketMQ 重投。
        // 券入队失败若被吞掉，消息会被误认为消费成功，而首单只有一次，用户再也拿不到邀请券。
        boolean granted = userService.grantInviteRewardOnFirstOrder(event.getUserId());
        if (granted) {
            log.info("首单邀请奖励事件已入队: userId={}", event.getUserId());
        }
    }
}
