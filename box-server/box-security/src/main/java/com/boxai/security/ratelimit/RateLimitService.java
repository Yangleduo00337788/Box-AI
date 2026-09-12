package com.boxai.security.ratelimit;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.infrastructure.redis.RedisService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final RedisService redisService;

    public RateLimitService(RedisService redisService) {
        this.redisService = redisService;
    }

    public void assertAllowed(String scope, String identifier, int maxAttempts, Duration window) {
        if (identifier == null || identifier.isBlank()) {
            identifier = "unknown";
        }
        String key = "box:ratelimit:" + scope + ":" + identifier;
        if (!redisService.incrementWithinLimit(key, maxAttempts, window)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
        }
    }
}
