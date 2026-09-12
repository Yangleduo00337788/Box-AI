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
        String codeKey = CODE_KEY_PREFIX + purpose.name() + ":" + normalized;
        redisService.set(codeKey, code, CODE_TTL);
        redisService.set(rateKey, "1", RATE_LIMIT);
        verificationEmailSender.send(normalized, purpose, code);
        log.info("Verification code sent for {} ({})", normalized, purpose);
        return exposeCode ? code : null;
    }

    public void verify(String email, VerificationCodePurpose purpose, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入验证码");
        }
        String normalized = normalizeEmail(email);
        String codeKey = CODE_KEY_PREFIX + purpose.name() + ":" + normalized;
        String stored = redisService.get(codeKey);
        if (stored == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!stored.equals(code.trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误");
        }
        redisService.delete(codeKey);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
