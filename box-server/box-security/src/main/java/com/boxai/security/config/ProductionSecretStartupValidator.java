package com.boxai.security.config;

import com.boxai.infrastructure.crypto.CryptoProperties;
import com.boxai.security.jwt.JwtProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class ProductionSecretStartupValidator implements ApplicationListener<ApplicationReadyEvent> {

    static final String DEFAULT_JWT_SECRET = "box-dev-jwt-secret-change-me-please-32b";
    static final String DEFAULT_AES_KEY = "box-dev-aes-256-key-change-me!!";

    private static final Set<String> NON_PRODUCTION_PROFILES = Set.of("dev", "test", "integration", "default");

    private final JwtProperties jwtProperties;
    private final CryptoProperties cryptoProperties;

    public ProductionSecretStartupValidator(JwtProperties jwtProperties, CryptoProperties cryptoProperties) {
        this.jwtProperties = jwtProperties;
        this.cryptoProperties = cryptoProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validate(event.getApplicationContext().getEnvironment());
    }

    void validate(Environment environment) {
        if (isNonProductionProfile(environment)) {
            return;
        }
        if (isDefaultSecret(jwtProperties.getSecret(), DEFAULT_JWT_SECRET)) {
            throw new IllegalStateException(
                    "生产环境必须配置 box.security.jwt.secret，不能使用默认值");
        }
        if (isDefaultSecret(cryptoProperties.getAesKey(), DEFAULT_AES_KEY)) {
            throw new IllegalStateException(
                    "生产环境必须配置 box.security.crypto.aes-key，不能使用默认值");
        }
    }

    private static boolean isNonProductionProfile(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return true;
        }
        return Arrays.stream(activeProfiles)
                .map(profile -> profile == null ? "" : profile.trim().toLowerCase())
                .allMatch(NON_PRODUCTION_PROFILES::contains);
    }

    private static boolean isDefaultSecret(String actual, String defaultValue) {
        if (actual == null || actual.isBlank()) {
            return true;
        }
        return defaultValue.equals(actual);
    }
}
