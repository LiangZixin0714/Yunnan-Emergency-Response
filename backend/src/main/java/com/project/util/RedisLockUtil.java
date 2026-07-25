package com.project.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLockUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockUtil.class);

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "emergency:lock:";
    private static final long DEFAULT_EXPIRE_TIME = 30;
    private static final long DEFAULT_WAIT_TIME = 10;

    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private static final ThreadLocal<String> lockValueHolder = new ThreadLocal<>();

    public RedisLockUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String generateLockValue() {
        return UUID.randomUUID().toString();
    }

    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    public boolean tryLock(String lockKey, long expireTime, TimeUnit timeUnit) {
        String key = LOCK_PREFIX + lockKey;
        String value = generateLockValue();
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, timeUnit);
            boolean locked = Boolean.TRUE.equals(result);
            if (locked) {
                lockValueHolder.set(value);
                logger.debug("获取锁成功: {}", key);
            }
            return locked;
        } catch (Exception e) {
            logger.error("获取锁失败: {}", key, e);
            return false;
        }
    }

    public boolean lock(String lockKey) {
        return lock(lockKey, DEFAULT_EXPIRE_TIME, DEFAULT_WAIT_TIME, TimeUnit.SECONDS);
    }

    public boolean lock(String lockKey, long expireTime, long waitTime, TimeUnit timeUnit) {
        String key = LOCK_PREFIX + lockKey;
        String value = generateLockValue();
        long waitMillis = timeUnit.toMillis(waitTime);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < waitMillis) {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(key, value, expireTime, timeUnit);
            if (Boolean.TRUE.equals(result)) {
                lockValueHolder.set(value);
                logger.debug("获取锁成功: {}", key);
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        logger.warn("获取锁超时: {}", key);
        return false;
    }

    public void unlock(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        String value = lockValueHolder.get();
        if (value == null) {
            logger.warn("当前线程未持有锁，无法释放: {}", key);
            return;
        }
        try {
            Long result = redisTemplate.execute(
                    UNLOCK_SCRIPT,
                    Collections.singletonList(key),
                    value
            );
            if (Long.valueOf(1).equals(result)) {
                logger.debug("释放锁成功: {}", key);
            } else {
                logger.warn("释放锁失败（锁可能已过期或不属于当前线程）: {}", key);
            }
        } catch (Exception e) {
            logger.error("释放锁失败: {}", key, e);
        } finally {
            lockValueHolder.remove();
        }
    }

    public void lockWithRetry(String lockKey, Runnable task) {
        lockWithRetry(lockKey, DEFAULT_EXPIRE_TIME, DEFAULT_WAIT_TIME, TimeUnit.SECONDS, 3, task);
    }

    public void lockWithRetry(String lockKey, long expireTime, long waitTime, TimeUnit timeUnit, int retryCount, Runnable task) {
        for (int i = 0; i < retryCount; i++) {
            if (lock(lockKey, expireTime, waitTime, timeUnit)) {
                try {
                    task.run();
                    return;
                } finally {
                    unlock(lockKey);
                }
            }
            if (i < retryCount - 1) {
                try {
                    Thread.sleep(timeUnit.toMillis(waitTime));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        throw new RuntimeException("获取锁失败，已重试" + retryCount + "次: " + lockKey);
    }
}
