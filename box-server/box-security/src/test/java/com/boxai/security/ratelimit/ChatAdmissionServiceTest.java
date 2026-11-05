package com.boxai.security.ratelimit;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.infrastructure.redis.RedisService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatAdmissionServiceTest {

    @Test
    void skipsWhenDisabled() {
        RedisService redisService = mock(RedisService.class);
        ChatAdmissionProperties properties = new ChatAdmissionProperties();
        properties.setEnabled(false);
        ChatAdmissionService service = new ChatAdmissionService(redisService, properties);

        service.acquire(null).close();

        verify(redisService, never()).eval(anyString(), anyList(), any(), any(), any(), any());
    }

    @Test
    void admitsWhenSlotFree() {
        RedisService redisService = mock(RedisService.class);
        when(redisService.eval(anyString(), anyList(), any(), any(), any(), any())).thenReturn(List.of(1, 0));
        ChatAdmissionService service = new ChatAdmissionService(redisService, new ChatAdmissionProperties());

        service.acquire(null).close();

        verify(redisService).evalLong(anyString(), anyList());
    }

    @Test
    void rejectsWhenQueueFull() {
        RedisService redisService = mock(RedisService.class);
        when(redisService.eval(anyString(), anyList(), any(), any(), any(), any())).thenReturn(List.of(-1, 800));
        ChatAdmissionService service = new ChatAdmissionService(redisService, new ChatAdmissionProperties());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.acquire(null));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
        assertEquals(ChatAdmissionService.BUSY_MESSAGE, ex.getMessage());
    }

    @Test
    void notifiesQueueThenPromotes() {
        RedisService redisService = mock(RedisService.class);
        when(redisService.eval(anyString(), anyList(), any(), any(), any(), any()))
                .thenReturn(List.of(0, 3));
        when(redisService.eval(anyString(), anyList(), any(), any(), any()))
                .thenReturn(List.of(1, 0));
        ChatAdmissionProperties properties = new ChatAdmissionProperties();
        properties.setQueueWaitSeconds(5);
        ChatAdmissionService service = new ChatAdmissionService(redisService, properties);
        AtomicInteger seen = new AtomicInteger();

        service.acquire(seen::set).close();

        assertEquals(3, seen.get());
    }
}
