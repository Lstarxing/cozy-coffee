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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 扫描并自动取消超时未支付订单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCancelJob {

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

    @Value("${cozy.order.timeout-cancel.enabled:true}")
    private boolean enabled;

    @Value("${cozy.order.timeout-cancel.timeout-minutes:1}")
    private int timeoutMinutes;

    @Value("${cozy.order.timeout-cancel.batch-size:200}")
    private int batchSize;

    @Value("${cozy.order.timeout-cancel.max-rounds:5}")
    private int maxRounds;

    @Scheduled(fixedDelayString = "${cozy.order.timeout-cancel.scan-interval-ms:60000}")
    public void autoCancelTimeoutOrders() {
        if (!enabled) {
            return;
        }

        String lockToken = UUID.randomUUID().toString();
        boolean locked = tryAcquireLock(lockToken);
        if (!locked) {
            return;
        }

        int totalScanned = 0;
        int totalCancelled = 0;
        int totalFailed = 0;

        try {
            for (int round = 0; round < maxRounds; round++) {
                List<Long> orderIds = fetchTimeoutPendingOrderIds();
                if (orderIds.isEmpty()) {
                    break;
                }

                totalScanned += orderIds.size();

                for (Long orderId : orderIds) {
                    try {
                        orderService.cancelOrder(orderId);
                        totalCancelled++;
                    } catch (Exception e) {
                        totalFailed++;
                        log.warn("自动取消超时订单失败: orderId={}, error={}", orderId, e.getMessage());
                    }
                }

                if (orderIds.size() < batchSize) {
                    break;
                }
            }

            if (totalScanned > 0 || totalFailed > 0) {
                log.info("超时订单自动取消任务完成: scanned={}, cancelled={}, failed={}, timeoutMinutes={}",
                        totalScanned,
                        totalCancelled,
                        totalFailed,
                        timeoutMinutes);
            }

            if (totalCancelled > 0) {
                evictAdminOrderCaches();
            }
        } finally {
            releaseLock(lockToken);
        }
    }

    private List<Long> fetchTimeoutPendingOrderIds() {
        List<Long> fromIndex = fetchTimeoutPendingOrderIdsFromZSet();
        if (!fromIndex.isEmpty()) {
            return fromIndex;
        }

        // 兼容兜底：迁移期或索引短暂不可用时，保留一次 DB 扫描路径。
        return fetchTimeoutPendingOrderIdsByDb();
    }

    private List<Long> fetchTimeoutPendingOrderIdsFromZSet() {
        long nowMillis = System.currentTimeMillis();
        Set<String> dueOrderIds = stringRedisTemplate.opsForZSet().rangeByScore(
                RedisKeyConstants.ORDER_PENDING_TIMEOUT_ZSET,
                0,
                nowMillis,
                0,
                batchSize);
        if (dueOrderIds == null || dueOrderIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>(dueOrderIds.size());
        for (String value : dueOrderIds) {
            Long orderId;
            try {
                orderId = Long.parseLong(value);
            } catch (Exception ex) {
                stringRedisTemplate.opsForZSet().remove(RedisKeyConstants.ORDER_PENDING_TIMEOUT_ZSET, value);
                continue;
            }

            ShopOrder order = orderMapper.selectById(orderId);
            if (order == null || !"pending".equals(order.getStatus()) || order.getCreatedAt() == null) {
                stringRedisTemplate.opsForZSet().remove(RedisKeyConstants.ORDER_PENDING_TIMEOUT_ZSET, value);
                continue;
            }

            LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
            if (order.getCreatedAt().isAfter(deadline)) {
                long correctedScore = order.getCreatedAt().plusMinutes(timeoutMinutes)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                stringRedisTemplate.opsForZSet().add(RedisKeyConstants.ORDER_PENDING_TIMEOUT_ZSET, value, correctedScore);
                continue;
            }

            result.add(orderId);
        }

        return result;
    }

    private List<Long> fetchTimeoutPendingOrderIdsByDb() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);

        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getStatus, "pending")
                .le(ShopOrder::getCreatedAt, deadline)
                .orderByAsc(ShopOrder::getCreatedAt)
                .last("LIMIT " + batchSize);

        List<ShopOrder> orders = orderMapper.selectList(wrapper);
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream().map(ShopOrder::getId).collect(Collectors.toList());
    }

    private boolean tryAcquireLock(String lockToken) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                    RedisKeyConstants.LOCK_ORDER_TIMEOUT_CANCEL_JOB,
                    lockToken,
                    55,
                    TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("获取超时取消任务锁失败", e);
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
                    Collections.singletonList(RedisKeyConstants.LOCK_ORDER_TIMEOUT_CANCEL_JOB),
                    lockToken);
        } catch (Exception e) {
            log.warn("释放超时取消任务锁失败", e);
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
