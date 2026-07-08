package com.cozy.gateway.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端订单/看板缓存清理器。
 * 抽离自 OrderController，便于 MQ 消费端在异步链路中复用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminOrderCacheEvictor {

    private static final String ADMIN_DASHBOARD_STATS_PREFIX = "cozy:admin:dashboard:stats:";
    private static final String ADMIN_ANALYTICS_TREND_PREFIX = "cozy:admin:analytics:trend:";
    private static final String ADMIN_ANALYTICS_DISTRIBUTION_PREFIX = "cozy:admin:analytics:distribution:";
    private static final String ADMIN_ANALYTICS_RANK_PREFIX = "cozy:admin:analytics:rank:";
    private static final String ADMIN_ORDERS_LIST_PREFIX = "cozy:admin:orders:list:";
    private static final String ADMIN_ORDERS_RECENT_PREFIX = "cozy:admin:orders:recent:";

    private final StringRedisTemplate stringRedisTemplate;

    public void evictAll() {
        evictByPrefix(ADMIN_ORDERS_LIST_PREFIX);
        evictByPrefix(ADMIN_ORDERS_RECENT_PREFIX);
        evictByPrefix(ADMIN_DASHBOARD_STATS_PREFIX);
        evictByPrefix(ADMIN_ANALYTICS_TREND_PREFIX);
        evictByPrefix(ADMIN_ANALYTICS_DISTRIBUTION_PREFIX);
        evictByPrefix(ADMIN_ANALYTICS_RANK_PREFIX);
    }

    private void evictByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return;
        }
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
            log.warn("清理管理端缓存失败: prefix={}", prefix, e);
        }
    }
}
