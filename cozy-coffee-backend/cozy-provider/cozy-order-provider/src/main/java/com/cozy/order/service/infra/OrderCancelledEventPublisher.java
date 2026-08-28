package com.cozy.order.service.infra;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCancelledEvent;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.mq.OutboxService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 订单取消事件投递（Outbox 模式，事务内原子提交保证消息不丢）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancelledEventPublisher {

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    /**
     * 统一构造 OrderCancelledEvent 写入 outbox 表（券回滚），与订单状态变更同一事务原子提交。
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
