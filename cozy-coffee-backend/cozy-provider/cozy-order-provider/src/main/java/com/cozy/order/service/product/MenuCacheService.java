package com.cozy.order.service.product;
import com.cozy.order.service.converter.OrderDtoConverter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.cozy.common.util.RedisLockUtil;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final LongAdder MENU_CACHE_HIT = new LongAdder();
    private static final LongAdder MENU_CACHE_MISS = new LongAdder();
    private static final LongAdder MENU_CACHE_EMPTY_HIT = new LongAdder();
    private static final LongAdder MENU_REBUILD_WAIT = new LongAdder();
    private static final LongAdder MENU_REBUILD_FALLBACK = new LongAdder();
    private static final AtomicLong MENU_METRIC_SEQ = new AtomicLong();
    private static final long L1_TTL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final long REBUILD_WAIT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(3);
    private static final long REBUILD_POLL_MIN_MS = 40L;
    private static final long REBUILD_POLL_MAX_MS = 80L;

    /** 分类展示顺序（设计文档 3.1）：01经典 → 02奶咖 → 03特调 → 04精品 → 05非咖啡 → 06烘焙 */
    private static final Map<String, Integer> CATEGORY_RANK = Map.of(
            "ESPRESSO", 1, "MILK", 2, "SIGNATURE", 3,
            "SPECIALTY", 4, "NON_COFFEE", 5, "BAKERY", 6);

    /**
     * 菜单排序：分类展示顺序 > 精品固定组合置尾 > sortOrder > id。
     * sort_order 是分类内计数器（每分类从 1 起），不能跨分类全局排序；
     * 一豆两喝/三喝为固定组合体验商品，置于精品 Bean 之后。
     */
    private static final Comparator<CoffeeProduct> MENU_ORDER =
            Comparator.comparingInt(MenuCacheService::categoryRank)
                    .thenComparingInt(MenuCacheService::specialtyTier)
                    .thenComparing(CoffeeProduct::getSortOrder,
                            Comparator.nullsLast(Comparator.<Integer>naturalOrder()))
                    .thenComparing(CoffeeProduct::getId);

    private static int categoryRank(CoffeeProduct p) {
        String category = p.getCategory();
        return CATEGORY_RANK.getOrDefault(category == null ? "" : category.toUpperCase(), 99);
    }

    private static int specialtyTier(CoffeeProduct p) {
        return "FIXED_COMBINATION".equalsIgnoreCase(p.getServingMode()) ? 1 : 0;
    }

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

        // Local single-flight: only one request per instance may rebuild. Followers wait on
        // this monitor and reuse the L1 result instead of returning a fake empty menu.
        synchronized (this) {
            List<CoffeeProductDTO> rebuiltLocally = freshLocalMenu();
            if (rebuiltLocally != null) {
                MENU_CACHE_HIT.increment();
                logMetricsMaybe();
                return rebuiltLocally;
            }

            List<CoffeeProductDTO> rebuiltRemotely = readRedisMenu();
            if (rebuiltRemotely != null) {
                return rebuiltRemotely;
            }

            String lockToken = UUID.randomUUID().toString();
            boolean locked = tryAcquireRebuildLock(RedisKeyConstants.LOCK_ORDER_MENU_REBUILD, lockToken, 8);
            try {
                if (!locked) {
                    MENU_REBUILD_WAIT.increment();
                    List<CoffeeProductDTO> waitedMenu = waitForRebuiltMenu();
                    if (waitedMenu != null) {
                        return waitedMenu;
                    }

                    // The lock holder may have failed. Retry ownership once; if another
                    // instance still owns it, perform one local fallback query rather than
                    // returning a semantically incorrect empty menu.
                    locked = tryAcquireRebuildLock(RedisKeyConstants.LOCK_ORDER_MENU_REBUILD, lockToken, 8);
                    if (!locked) {
                        MENU_REBUILD_FALLBACK.increment();
                        log.warn("等待菜单缓存重建超时，执行单实例数据库兜底查询");
                    }
                }

                // Close the race where another instance populated Redis immediately before
                // this instance acquired the distributed lock.
                List<CoffeeProductDTO> latestRedisMenu = readRedisMenu();
                if (latestRedisMenu != null) {
                    return latestRedisMenu;
                }

                List<CoffeeProductDTO> result = queryActiveProducts();
                storeMenu(result);
                return result;
            } finally {
                if (locked) {
                    releaseRebuildLock(RedisKeyConstants.LOCK_ORDER_MENU_REBUILD, lockToken);
                }
            }
        }
    }

    private List<CoffeeProductDTO> freshLocalMenu() {
        List<CoffeeProductDTO> menu = this.cachedMenu;
        return menu != null && System.currentTimeMillis() - this.cachedMenuAt < L1_TTL_MS ? menu : null;
    }

    private List<CoffeeProductDTO> readRedisMenu() {
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
        return null;
    }

    private List<CoffeeProductDTO> waitForRebuiltMenu() {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(REBUILD_WAIT_TIMEOUT_MS);
        while (System.nanoTime() < deadline) {
            try {
                long delay = ThreadLocalRandom.current().nextLong(
                        REBUILD_POLL_MIN_MS, REBUILD_POLL_MAX_MS + 1);
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }

            List<CoffeeProductDTO> localMenu = freshLocalMenu();
            if (localMenu != null) {
                MENU_CACHE_HIT.increment();
                logMetricsMaybe();
                return localMenu;
            }
            List<CoffeeProductDTO> redisMenu = readRedisMenu();
            if (redisMenu != null) {
                return redisMenu;
            }
        }
        return null;
    }

    private List<CoffeeProductDTO> queryActiveProducts() {
        LambdaQueryWrapper<CoffeeProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoffeeProduct::getStatus, "active");
        List<CoffeeProduct> products = productMapper.selectList(wrapper).stream()
                .sorted(MENU_ORDER)
                .collect(Collectors.toList());
        return dtoConverter.toProductDTOList(products);
    }

    private void storeMenu(List<CoffeeProductDTO> result) {
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
    }

    private boolean tryAcquireRebuildLock(String lockKey, String lockToken, int ttlSeconds) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, Duration.ofSeconds(ttlSeconds)));
    }

    private void releaseRebuildLock(String lockKey, String lockToken) {
        try {
            RedisLockUtil.releaseLock(stringRedisTemplate, lockKey, lockToken);
        } catch (Exception e) {
            log.warn("释放Redis重建锁失败: key={}", lockKey, e);
        }
    }

    private void logMetricsMaybe() {
        if (MENU_METRIC_SEQ.incrementAndGet() % 100 == 0) {
            log.info("菜单缓存指标: hit={} miss={} emptyHit={} rebuildWait={} rebuildFallback={}",
                    MENU_CACHE_HIT.sum(), MENU_CACHE_MISS.sum(),
                    MENU_CACHE_EMPTY_HIT.sum(), MENU_REBUILD_WAIT.sum(), MENU_REBUILD_FALLBACK.sum());
        }
    }
}
