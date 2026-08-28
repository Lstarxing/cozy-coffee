package com.cozy.order.service;

import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCancelledEvent;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.mq.OutboxService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

/**
 * 订单基础设施服务。
 * 从 OrderServiceImpl 抽出，统一承担订单相关的 Redis ZSet 超时索引维护
 * 和 Outbox 券回滚事件投递。
 *
 * 被 OrderCreationService / OrderCommandService 共享调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInfraService {

    private final StringRedisTemplate stringRedisTemplate;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Value("${cozy.order.timeout-cancel.timeout-minutes:1}")
    private int orderTimeoutMinutes;

    /**
     * 维护订单超时索引 ZSet：pending 订单写入 timeoutAt 分数，非 pending 移除。
     */
    public void syncPendingTimeoutIndex(ShopOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (!"pending".equalsIgnoreCase(order.getStatus()) || order.getCreatedAt() == null) {
            removePendingTimeoutIndex(order.getId());
            return;
        }
        try {
            long timeoutAtMillis = order.getCreatedAt()
                    .plusMinutes(orderTimeoutMinutes)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            stringRedisTemplate.opsForZSet().add(
                    RedisKeyConstants.ORDER_PENDING_TIMEOUT_ZSET,
                    String.valueOf(order.getId()),
                    timeoutAtMillis);
        } catch (Exception e) {
            log.warn("写入订单超时索引失败: orderId={}", order.getId(), e);
        }
    }

    public void removePendingTimeoutIndex(Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForZSet().remove(
                    RedisKeyConstants.ORDER_PENDING_TIMEOUT_ZSET,
                    String.valueOf(orderId));
        } catch (Exception e) {
            log.warn("移除订单超时索引失败: orderId={}", orderId, e);
        }
    }

    /**
     * v6.3: 统一构造 OrderCancelledEvent 写入 outbox 表。
     * 与订单状态变更在同一事务内原子提交，确保消息不丢。
     */
    public void publishCouponRollbackEvent(ShopOrder order) {
        Long mainCouponId = order.getAppliedCouponId();
        List<Long> addonCouponIds = parseAddonCouponIds(order);

        if (mainCouponId == null && addonCouponIds.isEmpty()) {
            log.info("订单未使用优惠券，无需回滚: orderId={}", order.getId());
            return;
        }

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .appliedCouponId(mainCouponId)
                .addonCouponIds(addonCouponIds)
                .occurredAt(LocalDateTime.now())
                .build();

        outboxService.publish(
                MqTopics.ORDER_EVENTS,
                MqTags.ORDER_CANCELLED,
                "coupon_rollback",
                order.getId(),
                event);
        log.info("OrderCancelledEvent 已写入 outbox: orderId={}, mainCoupon={}, addonCount={}",
                order.getId(), mainCouponId, addonCouponIds.size());
    }

    private List<Long> parseAddonCouponIds(ShopOrder order) {
        String json = order.getAppliedAddonCouponIds();
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<Long>>() {
                    });
        } catch (Exception e) {
            log.warn("解析附加券ID失败: orderId={}, error={}", order.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }
}
