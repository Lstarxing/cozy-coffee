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

    /** 正常取消：order.getId() 非空，无需 fallback */
    public void publishCouponRollbackEvent(ShopOrder order) {
        publishCouponRollbackEvent(order, null);
    }

    /** 订单落库失败（order.id 为 null）时用 fallbackAggregateId 作为非空聚合键，保证 outbox 行能写入 */
    public void publishCouponRollbackEvent(ShopOrder order, Long fallbackAggregateId) {
        Long mainCouponId = order.getAppliedCouponId();
        List<Long> addonCouponIds = parseAddonCouponIds(order);

        if (mainCouponId == null && addonCouponIds.isEmpty()) {
            log.info("订单未使用优惠券，无需回滚: orderId={}", order.getId());
            return;
        }
        Long aggregateId = order.getId() != null ? order.getId() : fallbackAggregateId;
        if (aggregateId == null) {
            log.error("券回滚缺少聚合键，跳过写入: orderNo={}, couponId={}", order.getOrderNo(), mainCouponId);
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
                aggregateId,
                event);
        log.info("OrderCancelledEvent 已写入 outbox: aggregateId={}, mainCoupon={}, addonCount={}",
                aggregateId, mainCouponId, addonCouponIds.size());
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
