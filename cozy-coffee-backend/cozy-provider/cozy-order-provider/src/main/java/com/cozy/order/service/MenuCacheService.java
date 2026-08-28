package com.cozy.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * 菜单三级缓存服务（Phase 4.6）。
 * L1 in-process (10min) + L2 Redis (5-8min TTL) + DB fallback。
 * 从 OrderServiceImpl 抽出，消除 1709 行上帝类中的缓存逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MenuCacheService {

    private static final String EMPTY_CACHE_MARKER = "__NULL__";
    private static final Semaphore MENU_DB_REBUILD_GUARD = new Semaphore(4);
    private static final LongAdder MENU_CACHE_HIT = new LongAdder();
    private static final LongAdder MENU_CACHE_MISS = new LongAdder();
    private static final LongAdder MENU_CACHE_EMPTY_HIT = new LongAdder();
    private static final LongAdder MENU_DEGRADE_FAST_FAIL = new LongAdder();
    private static final AtomicLong MENU_METRIC_SEQ = new AtomicLong();
    private static final long L1_TTL_MS = TimeUnit.MINUTES.toMillis(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final CoffeeProductMapper productMapper;
    private final OrderDtoConverter dtoConverter;

    private volatile List<CoffeeProductDTO> cachedMenu = null;
    private volatile long cachedMenuAt = 0;

    public void invalidate() {
        this.cachedMenu = null;
        this.cachedMenuAt = 0;
        try {
            redisTemplate.delete(RedisKeyConstants.ORDER_MENU_ACTIVE);
        } catch (Exception e) {
            log.warn("清理Redis菜单缓存失败", e);
        }
        log.info("菜单缓存已清除");
    }

    public List<CoffeeProductDTO> getMenu() {
        // L1: in-process cache
        List<CoffeeProductDTO> l1 = this.cachedMenu;
        if (l1 != null && System.currentTimeMillis() - this.cachedMenuAt < L1_TTL_MS) {
            MENU_CACHE_HIT.increment();
            logMetricsMaybe();
            return l1;
        }
        this.cachedMenu = null;
        this.cachedMenuAt = 0;

        // L2: Redis cache
        try {
            Object cachedValue = redisTemplate.opsForValue().get(RedisKeyConstants.ORDER_MENU_ACTIVE);
            if (EMPTY_CACHE_MARKER.equals(cachedValue)) {
                this.cachedMenu = Collections.emptyList();
                this.cachedMenuAt = System.currentTimeMillis();
                MENU_CACHE_EMPTY_HIT.increment();
                logMetricsMaybe();
                return this.cachedMenu;
            }
            List<CoffeeProductDTO> redisCached = dtoConverter.convertToCoffeeProductList(cachedValue);
            if (redisCached != null) {
                this.cachedMenu = redisCached;
                this.cachedMenuAt = System.currentTimeMillis();
                MENU_CACHE_HIT.increment();
                logMetricsMaybe();
                return redisCached;
            }
        } catch (Exception e) {
            log.warn("读取Redis菜单缓存失败，回退数据库查询", e);
        }
        MENU_CACHE_MISS.increment();

        // DB rebuild with distributed lock
        String lockToken = UUID.randomUUID().toString();
        boolean locked = tryAcquireRebuildLock(RedisKeyConstants.LOCK_ORDER_MENU_REBUILD, lockToken, 8);
        if (!locked) {
            try {
                TimeUnit.MILLISECONDS.sleep(40L);
                Object retryCache = redisTemplate.opsForValue().get(RedisKeyConstants.ORDER_MENU_ACTIVE);
                if (EMPTY_CACHE_MARKER.equals(retryCache)) {
                    this.cachedMenu = Collections.emptyList();
                    this.cachedMenuAt = System.currentTimeMillis();
                    MENU_CACHE_EMPTY_HIT.increment();
                    logMetricsMaybe();
                    return this.cachedMenu;
                }
                List<CoffeeProductDTO> redisCached = dtoConverter.convertToCoffeeProductList(retryCache);
                if (redisCached != null) {
                    this.cachedMenu = redisCached;
                    this.cachedMenuAt = System.currentTimeMillis();
                    MENU_CACHE_HIT.increment();
                    logMetricsMaybe();
                    return redisCached;
                }
            } catch (Exception e) {
                log.warn("重建等待后读取Redis菜单缓存失败", e);
            }
        }

        if (!acquireDbRebuildPermit()) {
            MENU_DEGRADE_FAST_FAIL.increment();
            logMetricsMaybe();
            return Collections.emptyList();
        }

        try {
            synchronized (this) {
                if (this.cachedMenu != null)
                    return this.cachedMenu;

                LambdaQueryWrapper<CoffeeProduct> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CoffeeProduct::getStatus, "active")
                        .orderByAsc(CoffeeProduct::getSortOrder);
                List<CoffeeProductDTO> result = productMapper.selectList(wrapper).stream()
                        .map(dtoConverter::toProductDTO)
                        .collect(Collectors.toList());

                this.cachedMenu = result;
                this.cachedMenuAt = System.currentTimeMillis();
                try {
                    if (result.isEmpty()) {
                        redisTemplate.opsForValue().set(
                                RedisKeyConstants.ORDER_MENU_ACTIVE, EMPTY_CACHE_MARKER,
                                Duration.ofSeconds(60));
                    } else {
                        long ttlMinutes = 5L + ThreadLocalRandom.current().nextLong(3L);
                        redisTemplate.opsForValue().set(
                                RedisKeyConstants.ORDER_MENU_ACTIVE, result,
                                Duration.ofMinutes(ttlMinutes));
                    }
                } catch (Exception e) {
                    log.warn("写入Redis菜单缓存失败", e);
                }
                log.info("菜单缓存已更新，共 {} 个商品", result.size());
                logMetricsMaybe();
                return result;
            }
        } finally {
            MENU_DB_REBUILD_GUARD.release();
            if (locked) {
                releaseRebuildLock(RedisKeyConstants.LOCK_ORDER_MENU_REBUILD, lockToken);
            }
        }
    }

    private boolean tryAcquireRebuildLock(String lockKey, String lockToken, int ttlSeconds) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, Duration.ofSeconds(ttlSeconds)));
    }

    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) else return 0 end";

    private void releaseRebuildLock(String lockKey, String lockToken) {
        try {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(RELEASE_LOCK_SCRIPT);
            redisScript.setResultType(Long.class);
            stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockToken);
        } catch (Exception e) {
            log.warn("释放Redis重建锁失败: key={}", lockKey, e);
        }
    }

    private boolean acquireDbRebuildPermit() {
        return MENU_DB_REBUILD_GUARD.tryAcquire();
    }

    private void logMetricsMaybe() {
        if (MENU_METRIC_SEQ.incrementAndGet() % 100 == 0) {
            log.info("菜单缓存指标: hit={} miss={} emptyHit={} fastFail={}",
                    MENU_CACHE_HIT.sum(), MENU_CACHE_MISS.sum(),
                    MENU_CACHE_EMPTY_HIT.sum(), MENU_DEGRADE_FAST_FAIL.sum());
        }
    }
}
