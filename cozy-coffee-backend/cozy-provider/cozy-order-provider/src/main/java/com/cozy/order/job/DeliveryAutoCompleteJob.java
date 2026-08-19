package com.cozy.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 外送订单到点自动完成。
 * 外送功能未上线，出餐后订单进入 delivering（配送中），
 * 本任务扫描预计送达时间已过且仍在配送中的订单，自动置为 completed 并发放积分/EXP。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAutoCompleteJob {

    private static final String ADMIN_CACHE_ORDERS_LIST_PREFIX = "cozy:admin:orders:list:";
    private static final String ADMIN_CACHE_ORDERS_RECENT_PREFIX = "cozy:admin:orders:recent:";
    private static final String ADMIN_CACHE_DASHBOARD_PREFIX = "cozy:admin:dashboard:stats:";
    private static final String ADMIN_CACHE_ANALYTICS_PREFIX = "cozy:admin:analytics:";
    private static final String LEGACY_ADMIN_CACHE_ORDERS_LIST_PREFIX = "admin:cache:orders:list:";
    private static final String LEGACY_ADMIN_CACHE_DASHBOARD_PREFIX = "admin:cache:dashboard:";
    private static final String LEGACY_ADMIN_CACHE_ANALYTICS_PREFIX = "admin:cache:analytics:";

    private static final int SEND_RETRY_MAX = 3;
    private static final long SEND_RETRY_BASE_DELAY_MS = 1000;

    private final ShopOrderMapper orderMapper;
    private final OrderServiceImpl orderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${cozy.order.delivery-auto-complete.enabled:true}")
    private boolean enabled;

    @Value("${cozy.order.delivery-auto-complete.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${cozy.order.delivery-auto-complete.scan-interval-ms:60000}")
    public void autoCompleteDeliveredOrders() {
        if (!enabled) {
            return;
        }

        String lockToken = UUID.randomUUID().toString();
        if (!tryAcquireLock(lockToken)) {
            return;
        }

        int totalCompleted = 0;
        int totalFailed = 0;
        try {
            List<Long> orderIds = fetchDueDeliveringOrderIds();
            for (Long orderId : orderIds) {
                try {
                    var order = orderService.completeDeliveredOrder(orderId);
                    publishCompleted(order);
                    totalCompleted++;
                } catch (Exception e) {
                    totalFailed++;
                    log.warn("配送中订单自动完成失败: orderId={}, error={}", orderId, e.getMessage());
                }
            }
            if (totalCompleted > 0 || totalFailed > 0) {
                log.info("配送订单自动完成任务: completed={}, failed={}", totalCompleted, totalFailed);
            }
            if (totalCompleted > 0) {
                evictAdminOrderCaches();
            }
        } finally {
            releaseLock(lockToken);
        }
    }

    /** status=delivering 且预计送达时间已过 */
    private List<Long> fetchDueDeliveringOrderIds() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getStatus, "delivering")
                .eq(ShopOrder::getDiningMethod, "DELIVERY")
                .isNotNull(ShopOrder::getExpectedDeliveryAt)
                .le(ShopOrder::getExpectedDeliveryAt, now)
                .orderByAsc(ShopOrder::getExpectedDeliveryAt)
                .last("LIMIT " + batchSize);

        List<ShopOrder> orders = orderMapper.selectList(wrapper);
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>(orders.size());
        for (ShopOrder o : orders) {
            ids.add(o.getId());
        }
        return ids;
    }

    /** 发放积分/EXP 事件：与 gateway OrderEventProducer 对等，失败重试。 */
    private void publishCompleted(ShopOrderDTO order) {
        if (order == null || order.getId() == null) {
            return;
        }
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .payAmount(order.getPayAmount())
                .expEarned(order.getExpEarned())
                .pointsEarned(order.getPointsEarned())
                .isFirstOrder(order.getIsFirstOrder())
                .hasNewProduct(order.getHasNewProduct())
                .isDelivery("DELIVERY".equals(order.getDiningMethod()))
                .occurredAt(LocalDateTime.now())
                .build();

        String destination = MqTopics.ORDER_EVENTS + ":" + MqTags.ORDER_COMPLETED;
        for (int attempt = 1; attempt <= SEND_RETRY_MAX; attempt++) {
            try {
                rocketMQTemplate.syncSend(
                        destination,
                        MessageBuilder.withPayload(event)
                                .setHeader("KEYS", String.valueOf(order.getId()))
                                .build());
                return;
            } catch (Exception e) {
                if (attempt == SEND_RETRY_MAX) {
                    log.error("配送自动完成派发 ORDER_COMPLETED 最终失败: orderId={}", order.getId(), e);
                } else {
                    long delay = SEND_RETRY_BASE_DELAY_MS * (1L << (attempt - 1));
                    log.warn("配送自动完成派发失败, attempt={}/{}, {}ms 后重试: orderId={}",
                            attempt, SEND_RETRY_MAX, delay, order.getId());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
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
            log.warn("获取配送自动完成任务锁失败", e);
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
            log.warn("释放配送自动完成任务锁失败", e);
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
