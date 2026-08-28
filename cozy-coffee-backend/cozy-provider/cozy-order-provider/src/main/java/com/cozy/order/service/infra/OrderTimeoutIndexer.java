package com.cozy.order.service.infra;

import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.order.entity.ShopOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

/**
 * 订单 pending 超时索引维护（Redis ZSet）。
 * 写超时索引，供 OrderTimeoutCancelJob 扫描自动取消超时未支付订单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutIndexer {

    private final StringRedisTemplate stringRedisTemplate;

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
}
