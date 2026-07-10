package com.cozy.gateway.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cozy.gateway.cache.AdminOrderCacheEvictor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 管理端缓存工具
 */
@Slf4j
public final class AdminCacheUtil {

    public static final long DASHBOARD_TTL_SECONDS = 45;
    public static final long ANALYTICS_TTL_SECONDS = 60;
    public static final long ORDER_LIST_TTL_SECONDS = 30;
    public static final long ORDER_RECENT_TTL_SECONDS = 20;
    private static final long JITTER_MAX_SECONDS = 8;

    public static final String DASHBOARD_STATS_PREFIX = "cozy:admin:dashboard:stats:";
    public static final String ANALYTICS_TREND_PREFIX = "cozy:admin:analytics:trend:";
    public static final String ANALYTICS_DISTRIBUTION_PREFIX = "cozy:admin:analytics:distribution:";
    public static final String ANALYTICS_RANK_PREFIX = "cozy:admin:analytics:rank:";
    public static final String ORDERS_LIST_PREFIX = "cozy:admin:orders:list:";
    public static final String ORDERS_RECENT_PREFIX = "cozy:admin:orders:recent:";

    private AdminCacheUtil() {
    }

    public static String buildKey(String prefix, Object... parts) {
        StringBuilder key = new StringBuilder(prefix);
        if (parts == null || parts.length == 0) {
            return key.toString();
        }
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                key.append(':');
            }
            key.append(parts[i] == null ? "_" : String.valueOf(parts[i]).trim());
        }
        return key.toString();
    }

    public static <T> T readCache(RedisTemplate<String, Object> redisTemplate,
                                   ObjectMapper objectMapper,
                                   String cacheKey,
                                   TypeReference<T> typeReference) {
        try {
            Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObj == null) {
                return null;
            }
            return objectMapper.convertValue(cachedObj, typeReference);
        } catch (Exception e) {
            log.warn("读取管理端缓存失败: cacheKey={}", cacheKey, e);
            return null;
        }
    }

    public static void writeCache(RedisTemplate<String, Object> redisTemplate,
                                   String cacheKey,
                                   Object value,
                                   long ttlSeconds) {
        try {
            long jitter = ttlSeconds > 1
                    ? ThreadLocalRandom.current().nextLong(Math.min(JITTER_MAX_SECONDS, ttlSeconds / 2) + 1)
                    : 0;
            redisTemplate.opsForValue().set(cacheKey, value, ttlSeconds + jitter, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入管理端缓存失败: cacheKey={}", cacheKey, e);
        }
    }

    public static void evictOrderAndAnalytics(AdminOrderCacheEvictor cacheEvictor) {
        cacheEvictor.evictByPrefix(ORDERS_LIST_PREFIX);
        cacheEvictor.evictByPrefix(ORDERS_RECENT_PREFIX);
        evictAnalytics(cacheEvictor);
    }

    public static void evictAnalytics(AdminOrderCacheEvictor cacheEvictor) {
        cacheEvictor.evictByPrefix(DASHBOARD_STATS_PREFIX);
        cacheEvictor.evictByPrefix(ANALYTICS_TREND_PREFIX);
        cacheEvictor.evictByPrefix(ANALYTICS_DISTRIBUTION_PREFIX);
        cacheEvictor.evictByPrefix(ANALYTICS_RANK_PREFIX);
    }
}
