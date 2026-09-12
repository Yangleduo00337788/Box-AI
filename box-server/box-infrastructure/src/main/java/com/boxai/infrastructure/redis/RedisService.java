package com.boxai.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 固定窗口计数：首次写入时设置 TTL，超过 maxAttempts 返回 false。
     */
    public boolean incrementWithinLimit(String key, int maxAttempts, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count <= maxAttempts;
    }

    public boolean ping() {
        String pong = redisTemplate.getConnectionFactory().getConnection().ping();
        return pong != null;
    }
}
