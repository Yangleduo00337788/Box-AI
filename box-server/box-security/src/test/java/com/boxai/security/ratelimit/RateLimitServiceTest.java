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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    @Test
    void throwsWhenLimitExceeded() {
        RedisService redisService = mock(RedisService.class);
        when(redisService.incrementWithinLimit(anyString(), anyInt(), any(Duration.class))).thenReturn(false);
        RateLimitService service = new RateLimitService(redisService, new RateLimitProperties());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertAllowed("login", "127.0.0.1", 10, Duration.ofMinutes(1)));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
    }

    @Test
    void skipsWhenDisabled() {
        RedisService redisService = mock(RedisService.class);
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(false);
        RateLimitService service = new RateLimitService(redisService, properties);
        service.assertAllowed("chat", "1:1", 60, Duration.ofMinutes(1));
        verify(redisService, never()).incrementWithinLimit(anyString(), anyInt(), any(Duration.class));
    }

    @Test
    void usesConfiguredChatLimit() {
        RedisService redisService = mock(RedisService.class);
        when(redisService.incrementWithinLimit(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        RateLimitProperties properties = new RateLimitProperties();
        properties.setChatPerMinute(10000);
        RateLimitService service = new RateLimitService(redisService, properties);
        service.assertAllowed("chat", "1:1", 60, Duration.ofMinutes(1));
        verify(redisService).incrementWithinLimit(eq("box:ratelimit:chat:1:1"), eq(10000), any(Duration.class));
    }
}
