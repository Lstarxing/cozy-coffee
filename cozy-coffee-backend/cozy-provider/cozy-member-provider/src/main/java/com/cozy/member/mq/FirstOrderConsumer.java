package com.cozy.member.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.FirstOrderRewardConfig;
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
 * 订单完成 → 首单奖励（+200 积分）。
 *
 * <p>CLUSTERING 模式，幂等检查 PointsLot.sourceType='first_order_bonus'。
 *
 * <p>「邀请奖励资格认领」自 ADR 0001 §8 第 7 步起改由 **user 侧自己消费 `ORDER_COMPLETED`**
 * （`OrderCompletedInviteConsumer`）—— 本消费者不再通过 Dubbo 回调 user，
 * `member → user` 这条反向写边已删除。两个域各管各的：首单积分属 member，邀请资格属 user。
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

    @Override
    public void onMessage(OrderCompletedEvent event) {
        if (!Boolean.TRUE.equals(event.getIsFirstOrder())) {
            return;
        }

        // 靠 points_lots 的 (user_id, source_type, source_id) 幂等：先查后插，并发由唯一键兜底。
        // 重投时这里直接返回是安全的 —— 本消费者现在只管积分这一件事（邀请资格认领已移到 user 侧）。
        long count = pointsLotMapper.selectCount(new LambdaQueryWrapper<PointsLot>()
                .eq(PointsLot::getSourceType, firstOrderRewardConfig.getSourceType())
                .eq(PointsLot::getSourceId, event.getOrderId()));
        if (count > 0) {
            log.debug("首单奖励已发放，跳过积分写入: orderId={}", event.getOrderId());
            return;
        }
        try {
            memberService.addPointsWithLot(event.getUserId(), firstOrderRewardConfig.getPoints(),
                    firstOrderRewardConfig.getSourceType(),
                    event.getOrderId(), "新用户首单奖励");
            log.info("首单奖励发放成功: userId={}, orderId={}", event.getUserId(), event.getOrderId());
        } catch (DuplicateKeyException e) {
            log.info("首单奖励并发发放被拦截(幂等): orderId={}", event.getOrderId());
        }
    }
}
