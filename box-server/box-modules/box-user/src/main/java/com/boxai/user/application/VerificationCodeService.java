package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.infrastructure.redis.RedisService;
import com.boxai.user.support.VerificationCodePurpose;
import com.boxai.user.support.VerificationEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RATE_LIMIT = Duration.ofSeconds(60);
    private static final String CODE_KEY_PREFIX = "box:auth:code:";
    private static final String RATE_KEY_PREFIX = "box:auth:code:rate:";
    private static final String ATTEMPT_KEY_PREFIX = "box:auth:code:attempt:";
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final RedisService redisService;
    private final VerificationEmailSender verificationEmailSender;
    private final SecureRandom random = new SecureRandom();
    private final boolean exposeCode;

    public VerificationCodeService(RedisService redisService,
                                   VerificationEmailSender verificationEmailSender,
                                   @Value("${box.auth.verification.expose-code:false}") boolean exposeCode) {
        this.redisService = redisService;
        this.verificationEmailSender = verificationEmailSender;
        this.exposeCode = exposeCode;
    }

    public String send(String email, VerificationCodePurpose purpose) {
        String normalized = normalizeEmail(email);
        String rateKey = RATE_KEY_PREFIX + purpose.name() + ":" + normalized;
        if (redisService.get(rateKey) != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        String codeKey = codeKey(purpose, normalized);
        String attemptKey = attemptKey(purpose, normalized);
        redisService.delete(attemptKey);
        redisService.set(codeKey, code, CODE_TTL);
        try {
            verificationEmailSender.send(normalized, purpose, code);
        } catch (RuntimeException ex) {
            redisService.delete(codeKey);
            throw ex;
        }
        redisService.set(rateKey, "1", RATE_LIMIT);
        log.info("Verification code sent for {} ({})", normalized, purpose);
        return exposeCode ? code : null;
    }

    public void verify(String email, VerificationCodePurpose purpose, String code) {
        verify(email, purpose, code, true);
    }

    public void verify(String email, VerificationCodePurpose purpose, String code, boolean consume) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入验证码");
        }
        String normalized = normalizeEmail(email);
        String stored = redisService.get(codeKey(purpose, normalized));
        if (stored == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!stored.equals(code.trim())) {
            String attemptKey = attemptKey(purpose, normalized);
            if (!redisService.incrementWithinLimit(attemptKey, MAX_VERIFY_ATTEMPTS, CODE_TTL)) {
                redisService.delete(codeKey(purpose, normalized));
                throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码尝试次数过多，请重新获取");
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误");
        }
        if (consume) {
            consume(normalized, purpose);
        }
    }

    public void consume(String email, VerificationCodePurpose purpose) {
        String normalized = normalizeEmail(email);
        redisService.delete(codeKey(purpose, normalized));
        redisService.delete(attemptKey(purpose, normalized));
    }

    private static String codeKey(VerificationCodePurpose purpose, String normalizedEmail) {
        return CODE_KEY_PREFIX + purpose.name() + ":" + normalizedEmail;
    }

    private static String attemptKey(VerificationCodePurpose purpose, String normalizedEmail) {
        return ATTEMPT_KEY_PREFIX + purpose.name() + ":" + normalizedEmail;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
