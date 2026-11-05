package com.boxai.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

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

    public long increment(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        return count == null ? 0L : count;
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

    @SuppressWarnings("unchecked")
    public List<Object> eval(String lua, List<String> keys, String... args) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(List.class);
        List<Object> result = redisTemplate.execute(script, keys, (Object[]) args);
        return result == null ? List.of() : result;
    }

    public long evalLong(String lua, List<String> keys, String... args) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        Long value = redisTemplate.execute(script, keys, (Object[]) args);
        return value == null ? 0L : value;
    }
}
