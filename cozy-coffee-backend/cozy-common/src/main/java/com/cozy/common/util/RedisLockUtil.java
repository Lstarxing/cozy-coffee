package com.cozy.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * Redis 分布式锁工具。
 * <p>
 * 抢占用 {@code setIfAbsent(key, token, ttl)}（token 随机值），释放用下方 Lua 脚本
 * "比对 token 后 del"，把两步合成一次原子操作，避免 TOCTOU：
 * 锁超时被其它持有者重新抢走后，原持有者迟到的 {@code del} 会误删他人锁，破坏互斥。
 */
public final class RedisLockUtil {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private RedisLockUtil() {
    }

    /**
     * 安全释放分布式锁：仅当 key 当前值等于 lockToken 时才删除。
     *
     * @return true = 锁确实被本调用删除；false = key 不存在或值不匹配（锁非本次持有）
     */
    public static boolean releaseLock(StringRedisTemplate redis, String lockKey, String lockToken) {
        Long result = redis.execute(RELEASE_SCRIPT, Collections.singletonList(lockKey), lockToken);
        return result != null && result > 0;
    }
}
