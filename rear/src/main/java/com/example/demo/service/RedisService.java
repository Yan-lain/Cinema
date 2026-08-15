package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Redis服务类
 * 
 * 【架构说明】
 * 封装Redis的常用操作，提供统一的接口供其他模块使用
 * 
 * 【核心功能】
 * 1. 基础缓存操作（set、get、delete、exists）
 * 2. Hash结构操作（hset、hget）
 * 3. 原子操作（increment、decrement）
 * 4. 分布式锁（tryLock、unlock）
 * 
 * 【安全风险】
 * 1. 异常处理简单：使用System.err打印错误，生产环境应使用日志框架
 * 2. 分布式锁实现不完美：存在ABA问题和锁过期问题
 * 3. 无缓存穿透防护：大量请求不存在的Key可能导致数据库压力
 * 
 * 【改进建议】
 * 1. 使用SLF4J日志框架替代System.err
 * 2. 实现更完善的分布式锁（使用Lua脚本）
 * 3. 添加缓存穿透防护（布隆过滤器）
 */
@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final String NULL_VALUE = "__NULL__";
    private static final long NULL_TTL_SECONDS = 60;

    private final ReentrantLock localLock = new ReentrantLock();

    // 分布式锁Lua脚本
    // 用于释放锁，确保只有当前线程才能释放锁
    private final DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    // ============ 基础缓存操作 ============

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            long adjustedTimeout = addRandomOffset(timeout, unit);
            redisTemplate.opsForValue().set(key, value, adjustedTimeout, unit);
            logger.debug("Redis set success: key={}, timeout={} {}", key, adjustedTimeout, unit);
        } catch (Exception e) {
            logger.warn("Redis set error: key={}, error={}", key, e.getMessage());
        }
    }

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            logger.debug("Redis set success: key={}", key);
        } catch (Exception e) {
            logger.warn("Redis set error: key={}, error={}", key, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            return clazz.cast(value);
        } catch (Exception e) {
            logger.warn("Redis get error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            return value;
        } catch (Exception e) {
            logger.warn("Redis get error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            logger.debug("Redis delete success: key={}", key);
        } catch (Exception e) {
            logger.warn("Redis delete error: key={}, error={}", key, e.getMessage());
        }
    }

    public void delete(Set<String> keys) {
        try {
            redisTemplate.delete(keys);
            logger.debug("Redis delete success: keys count={}", keys.size());
        } catch (Exception e) {
            logger.warn("Redis delete error: keys count={}, error={}", keys.size(), e.getMessage());
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.warn("Redis exists error: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    // ============ Hash结构操作 ============

    public void hset(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            logger.debug("Redis hset success: key={}, hashKey={}", key, hashKey);
        } catch (Exception e) {
            logger.warn("Redis hset error: key={}, hashKey={}, error={}", key, hashKey, e.getMessage());
        }
    }

    public void hmset(String key, Map<String, Object> hash) {
        try {
            redisTemplate.opsForHash().putAll(key, hash);
            logger.debug("Redis hmset success: key={}, hash size={}", key, hash.size());
        } catch (Exception e) {
            logger.warn("Redis hmset error: key={}, error={}", key, e.getMessage());
        }
    }

    public Object hget(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            logger.warn("Redis hget error: key={}, hashKey={}, error={}", key, hashKey, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T hget(String key, String hashKey, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForHash().get(key, hashKey);
            return value != null ? clazz.cast(value) : null;
        } catch (Exception e) {
            logger.warn("Redis hget error: key={}, hashKey={}, error={}", key, hashKey, e.getMessage());
            return null;
        }
    }

    public Map<Object, Object> hgetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            logger.warn("Redis hgetAll error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public Long hdel(String key, String... hashKeys) {
        try {
            return redisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
        } catch (Exception e) {
            logger.warn("Redis hdel error: key={}, error={}", key, e.getMessage());
            return 0L;
        }
    }

    // ============ List结构操作 ============

    public Long lpush(String key, Object... values) {
        try {
            return redisTemplate.opsForList().leftPushAll(key, values);
        } catch (Exception e) {
            logger.warn("Redis lpush error: key={}, error={}", key, e.getMessage());
            return 0L;
        }
    }

    public Long rpush(String key, Object... values) {
        try {
            return redisTemplate.opsForList().rightPushAll(key, values);
        } catch (Exception e) {
            logger.warn("Redis rpush error: key={}, error={}", key, e.getMessage());
            return 0L;
        }
    }

    public Object lpop(String key) {
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            logger.warn("Redis lpop error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public Object rpop(String key) {
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            logger.warn("Redis rpop error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public List<Object> lrange(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            logger.warn("Redis lrange error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    // ============ Set结构操作 ============

    public Long sadd(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            logger.warn("Redis sadd error: key={}, error={}", key, e.getMessage());
            return 0L;
        }
    }

    public Set<Object> smembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            logger.warn("Redis smembers error: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public Boolean sismember(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            logger.warn("Redis sismember error: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public Long srem(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            logger.warn("Redis srem error: key={}, error={}", key, e.getMessage());
            return 0L;
        }
    }

    // ============ 原子操作 ============

    public long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            logger.warn("Redis increment error: key={}, delta={}, error={}", key, delta, e.getMessage());
            return -1;
        }
    }

    public long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            logger.warn("Redis decrement error: key={}, delta={}, error={}", key, delta, e.getMessage());
            return -1;
        }
    }

    // ============ 分布式锁 ============

    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        try {
            String lockKey = LOCK_PREFIX + key;
            boolean success = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, value, timeout, unit));
            if (success) {
                logger.debug("Acquired distributed lock: key={}", lockKey);
            }
            return success;
        } catch (Exception e) {
            logger.warn("Redis tryLock error: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public boolean unlock(String key, String value) {
        try {
            String lockKey = LOCK_PREFIX + key;
            Long result = redisTemplate.execute(unlockScript, Arrays.asList(lockKey), value);
            boolean success = result != null && result > 0;
            if (success) {
                logger.debug("Released distributed lock: key={}", lockKey);
            } else {
                logger.warn("Failed to release lock, may have been expired: key={}", lockKey);
            }
            return success;
        } catch (Exception e) {
            logger.warn("Redis unlock error: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public boolean tryLockWithRetry(String key, String value, long timeout, TimeUnit unit, int maxRetries, long retryDelayMs) {
        int attempts = 0;
        while (attempts < maxRetries) {
            if (tryLock(key, value, timeout, unit)) {
                return true;
            }
            attempts++;
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Lock retry interrupted");
                return false;
            }
        }
        logger.warn("Failed to acquire lock after {} attempts: key={}", maxRetries, key);
        return false;
    }

    // ============ 缓存穿透防护 ============

    public void cacheNullValue(String key) {
        try {
            redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_TTL_SECONDS, TimeUnit.SECONDS);
            logger.debug("Cached null value: key={}", key);
        } catch (Exception e) {
            logger.warn("Cache null value error: key={}, error={}", key, e.getMessage());
        }
    }

    public boolean isNullValue(Object value) {
        return NULL_VALUE.equals(value);
    }

    // ============ 缓存击穿防护 ============

    public <T> T getWithCacheBreakdownProtection(String key, Class<T> clazz, CacheLoader<T> loader, 
                                                   long timeout, TimeUnit unit) {
        Object value = get(key);
        if (value != null) {
            return clazz.cast(value);
        }

        localLock.lock();
        try {
            value = get(key);
            if (value != null) {
                return clazz.cast(value);
            }

            T result = loader.load();
            if (result != null) {
                set(key, result, timeout, unit);
            } else {
                cacheNullValue(key);
            }
            return result;
        } catch (Exception e) {
            logger.error("Cache breakdown protection error: key={}, error={}", key, e.getMessage());
            return loader.load();
        } finally {
            localLock.unlock();
        }
    }

    // ============ 其他操作 ============

    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            logger.warn("Redis expire error: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            logger.warn("Redis getExpire error: key={}, error={}", key, e.getMessage());
            return -1L;
        }
    }

    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            logger.warn("Redis keys error: pattern={}, error={}", pattern, e.getMessage());
            return null;
        }
    }

    public void flushDb() {
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.flushDb();
                return null;
            });
            logger.info("Redis flushDb completed");
        } catch (Exception e) {
            logger.error("Redis flushDb error: {}", e.getMessage());
        }
    }

    // ============ 辅助方法 ============

    private long addRandomOffset(long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            return timeout;
        }
        long millis = unit.toMillis(timeout);
        double offset = millis * 0.1;
        long randomOffset = (long) (Math.random() * offset * 2 - offset);
        return TimeUnit.MILLISECONDS.convert(millis + randomOffset, TimeUnit.MILLISECONDS);
    }

    public interface CacheLoader<T> {
        T load();
    }
}
