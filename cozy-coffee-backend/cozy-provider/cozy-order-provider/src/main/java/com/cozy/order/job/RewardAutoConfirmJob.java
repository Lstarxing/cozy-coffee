package com.cozy.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 订单奖励自动确认（兜底，替代原 DeliveryAutoCompleteJob）：
 * - 自提：completed && !rewards_granted && completed_at + pickupGrace <= now → 自动「确认取餐」→ 发放积分/EXP
 * - 外送：delivering && !rewards_granted && expectedDeliveryAt + deliveryGrace <= now → 自动「确认收货」→ 发放积分/EXP
 * <p>
 * 发奖由 OrderCommandService.grantRewards 的 CAS 完成并统一发布 ORDER_COMPLETED，
 * 本任务只触发 grantRewards，不重复发布；与用户手动确认天然互斥（CAS 只允许一个赢家）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RewardAutoConfirmJob {

    private static final String ADMIN_CACHE_ORDERS_LIST_PREFIX = "cozy:admin:orders:list:";
    private static final String ADMIN_CACHE_ORDERS_RECENT_PREFIX = "cozy:admin:orders:recent:";
    private static final String ADMIN_CACHE_DASHBOARD_PREFIX = "cozy:admin:dashboard:stats:";
    private static final String ADMIN_CACHE_ANALYTICS_PREFIX = "cozy:admin:analytics:";
    private static final String LEGACY_ADMIN_CACHE_ORDERS_LIST_PREFIX = "admin:cache:orders:list:";
    private static final String LEGACY_ADMIN_CACHE_DASHBOARD_PREFIX = "admin:cache:dashboard:";
    private static final String LEGACY_ADMIN_CACHE_ANALYTICS_PREFIX = "admin:cache:analytics:";

    private final ShopOrderMapper orderMapper;
    private final OrderServiceImpl orderService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${cozy.order.reward-auto-confirm.enabled:true}")
    private boolean enabled;

    @Value("${cozy.order.reward-auto-confirm.batch-size:100}")
    private int batchSize;

    @Value("${cozy.order.reward-auto-confirm.delivery-grace-minutes:60}")
    private int deliveryGraceMinutes;

    @Value("${cozy.order.reward-auto-confirm.pickup-grace-minutes:60}")
    private int pickupGraceMinutes;

    @Scheduled(fixedDelayString = "${cozy.order.reward-auto-confirm.scan-interval-ms:60000}")
    public void autoConfirmRewards() {
        if (!enabled) {
            return;
        }

        String lockToken = UUID.randomUUID().toString();
        if (!tryAcquireLock(lockToken)) {
            return;
        }

        int totalConfirmed = 0;
        int totalFailed = 0;
        try {
            for (Long orderId : fetchDuePickupOrderIds()) {
                try {
                    orderService.grantRewards(orderId);
                    totalConfirmed++;
                } catch (Exception e) {
                    totalFailed++;
                    log.warn("自提奖励自动确认失败: orderId={}, error={}", orderId, e.getMessage());
                }
            }
            for (Long orderId : fetchDueDeliveringOrderIds()) {
                try {
                    orderService.grantRewards(orderId);
                    totalConfirmed++;
                } catch (Exception e) {
                    totalFailed++;
                    log.warn("外送奖励自动确认失败: orderId={}, error={}", orderId, e.getMessage());
                }
            }
            if (totalConfirmed > 0 || totalFailed > 0) {
                log.info("奖励自动确认任务: confirmed={}, failed={}", totalConfirmed, totalFailed);
            }
            if (totalConfirmed > 0) {
                evictAdminOrderCaches();
            }
        } finally {
            releaseLock(lockToken);
        }
    }

    /** 自提：已出餐(completed) 且未发奖 且 出餐后超过 pickupGraceMinutes */
    private List<Long> fetchDuePickupOrderIds() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pickupGraceMinutes);
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getStatus, "completed")
                .eq(ShopOrder::getRewardsGranted, false)
                .ne(ShopOrder::getDiningMethod, "DELIVERY")
                .isNotNull(ShopOrder::getCompletedAt)
                .le(ShopOrder::getCompletedAt, cutoff)
                .orderByAsc(ShopOrder::getCompletedAt)
                .last("LIMIT " + batchSize);

        return toIds(orderMapper.selectList(wrapper));
    }

    /** 外送：配送中(delivering) 未发奖 且 预计送达后超过 deliveryGraceMinutes */
    private List<Long> fetchDueDeliveringOrderIds() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(deliveryGraceMinutes);
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getStatus, "delivering")
                .eq(ShopOrder::getRewardsGranted, false)
                .isNotNull(ShopOrder::getExpectedDeliveryAt)
                .le(ShopOrder::getExpectedDeliveryAt, cutoff)
                .orderByAsc(ShopOrder::getExpectedDeliveryAt)
                .last("LIMIT " + batchSize);

        return toIds(orderMapper.selectList(wrapper));
    }

    private List<Long> toIds(List<ShopOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        return orders.stream().map(ShopOrder::getId).toList();
    }

    private boolean tryAcquireLock(String lockToken) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                    RedisKeyConstants.LOCK_ORDER_DELIVERY_AUTO_COMPLETE_JOB,
                    lockToken,
                    55,
                    TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("获取奖励自动确认任务锁失败", e);
            return false;
        }
    }

    private void releaseLock(String lockToken) {
        try {
            String releaseScript = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end";
            org.springframework.data.redis.core.script.DefaultRedisScript<Long> redisScript =
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>();
            redisScript.setScriptText(releaseScript);
            redisScript.setResultType(Long.class);
            stringRedisTemplate.execute(
                    redisScript,
                    Collections.singletonList(RedisKeyConstants.LOCK_ORDER_DELIVERY_AUTO_COMPLETE_JOB),
                    lockToken);
        } catch (Exception e) {
            log.warn("释放奖励自动确认任务锁失败", e);
        }
    }

    private void evictAdminOrderCaches() {
        evictByPrefix(ADMIN_CACHE_ORDERS_LIST_PREFIX);
        evictByPrefix(ADMIN_CACHE_ORDERS_RECENT_PREFIX);
        evictByPrefix(ADMIN_CACHE_DASHBOARD_PREFIX);
        evictByPrefix(ADMIN_CACHE_ANALYTICS_PREFIX);
        evictByPrefix(LEGACY_ADMIN_CACHE_ORDERS_LIST_PREFIX);
        evictByPrefix(LEGACY_ADMIN_CACHE_DASHBOARD_PREFIX);
        evictByPrefix(LEGACY_ADMIN_CACHE_ANALYTICS_PREFIX);
    }

    private void evictByPrefix(String prefix) {
        String pattern = prefix + "*";
        List<String> batch = new ArrayList<>(200);
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 200) {
                        stringRedisTemplate.delete(batch);
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                stringRedisTemplate.delete(batch);
            }
        } catch (Exception e) {
            log.warn("清理缓存失败: prefix={}", prefix, e);
        }
    }
}
