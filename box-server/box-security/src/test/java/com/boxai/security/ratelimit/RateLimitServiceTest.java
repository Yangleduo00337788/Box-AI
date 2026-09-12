package com.boxai.security.ratelimit;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.infrastructure.redis.RedisService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    @Test
    void throwsWhenLimitExceeded() {
        RedisService redisService = mock(RedisService.class);
        when(redisService.incrementWithinLimit(anyString(), anyInt(), any(Duration.class))).thenReturn(false);
        RateLimitService service = new RateLimitService(redisService);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertAllowed("login", "127.0.0.1", 10, Duration.ofMinutes(1)));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
    }
}
