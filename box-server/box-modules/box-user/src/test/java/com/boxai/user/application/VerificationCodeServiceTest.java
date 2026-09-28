package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.infrastructure.redis.RedisService;
import com.boxai.user.support.VerificationCodePurpose;
import com.boxai.user.support.VerificationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private RedisService redisService;
    @Mock
    private VerificationEmailSender verificationEmailSender;

    private VerificationCodeService service;

    @BeforeEach
    void setUp() {
        service = new VerificationCodeService(redisService, verificationEmailSender, false);
    }

    @Test
    void verifyLocksOutAfterTooManyWrongAttempts() {
        String email = "user@example.com";
        String codeKey = "box:auth:code:REGISTER:" + email;
        String attemptKey = "box:auth:code:attempt:REGISTER:" + email;
        when(redisService.get(codeKey)).thenReturn("123456");
        when(redisService.incrementWithinLimit(eq(attemptKey), eq(5), any(Duration.class))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verify(email, VerificationCodePurpose.REGISTER, "000000"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        assertEquals("验证码尝试次数过多，请重新获取", ex.getMessage());
        verify(redisService).delete(codeKey);
    }

    @Test
    void verifyIncrementsAttemptsOnWrongCode() {
        String email = "user@example.com";
        String codeKey = "box:auth:code:REGISTER:" + email;
        String attemptKey = "box:auth:code:attempt:REGISTER:" + email;
        when(redisService.get(codeKey)).thenReturn("123456");
        when(redisService.incrementWithinLimit(eq(attemptKey), eq(5), any(Duration.class))).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verify(email, VerificationCodePurpose.REGISTER, "000000"));
        assertEquals("验证码错误", ex.getMessage());
        verify(redisService, never()).delete(codeKey);
    }

    @Test
    void verifyClearsCodeAndAttemptsOnSuccess() {
        String email = "user@example.com";
        String codeKey = "box:auth:code:REGISTER:" + email;
        String attemptKey = "box:auth:code:attempt:REGISTER:" + email;
        when(redisService.get(codeKey)).thenReturn("123456");

        service.verify(email, VerificationCodePurpose.REGISTER, "123456");

        verify(redisService).delete(codeKey);
        verify(redisService).delete(attemptKey);
    }
}
