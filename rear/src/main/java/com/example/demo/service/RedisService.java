package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            // Redis不可用时不影响主业务
            System.err.println("Redis set error: " + e.getMessage());
        }
    }

    /**
     * 设置缓存（永久）
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            System.err.println("Redis set error: " + e.getMessage());
        }
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            System.err.println("Redis get error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("Redis delete error: " + e.getMessage());
        }
    }

    /**
     * 判断缓存是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            System.err.println("Redis exists error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hash设置
     */
    public void hset(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            System.err.println("Redis hset error: " + e.getMessage());
        }
    }

    /**
     * Hash获取
     */
    public Object hget(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            System.err.println("Redis hget error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            System.err.println("Redis expire error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 原子递增
     */
    public long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            System.err.println("Redis increment error: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 原子递减
     */
    public long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            System.err.println("Redis decrement error: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 分布式锁（获取锁）
     * @param key 锁的key
     * @param value 锁的值（通常用UUID）
     * @param timeout 锁的过期时间
     * @param unit 时间单位
     * @return 是否获取到锁
     */
    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
        } catch (Exception e) {
            System.err.println("Redis tryLock error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 分布式锁（释放锁）
     */
    public void unlock(String key, String value) {
        try {
            Object currentValue = redisTemplate.opsForValue().get(key);
            if (value.equals(currentValue)) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            System.err.println("Redis unlock error: " + e.getMessage());
        }
    }
}
