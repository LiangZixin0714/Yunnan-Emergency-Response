package com.project.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Component
public class RedisLockUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockUtil.class);

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "emergency:lock:";
    private static final long DEFAULT_EXPIRE_TIME = 30;
    private static final long DEFAULT_WAIT_TIME = 10;

    public RedisLockUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    public boolean tryLock(String lockKey, long expireTime, TimeUnit timeUnit) {
        String key = LOCK_PREFIX + lockKey;
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", expireTime, timeUnit);
            boolean locked = Boolean.TRUE.equals(result);
            if (locked) {
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
        long waitMillis = timeUnit.toMillis(waitTime);
        long expireMillis = timeUnit.toMillis(expireTime);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < waitMillis) {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", expireMillis, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(result)) {
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
        try {
            redisTemplate.delete(key);
            logger.debug("释放锁成功: {}", key);
        } catch (Exception e) {
            logger.error("释放锁失败: {}", key, e);
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