package com.boxai.bootstrap.config;

import com.boxai.user.config.OAuthProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class ProductionRuntimeGuard implements ApplicationListener<ApplicationReadyEvent> {

    private static final Set<String> NON_PRODUCTION_PROFILES = Set.of("dev", "test", "integration", "default");

    private final OAuthProperties oauthProperties;

    public ProductionRuntimeGuard(OAuthProperties oauthProperties) {
        this.oauthProperties = oauthProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validate(event.getApplicationContext().getEnvironment());
    }

    void validate(Environment environment) {
        if (isNonProductionProfile(environment)) {
            return;
        }
        String base = oauthProperties.getPublicBaseUrl();
        if (base == null || base.isBlank() || containsLocalHost(base)) {
            throw new IllegalStateException("生产环境必须配置 box.oauth.public-base-url 为公网后端地址，不能使用 localhost");
        }
        String origins = oauthProperties.getAllowedRedirectOrigins();
        if (origins == null || origins.isBlank()) {
            throw new IllegalStateException("生产环境必须配置 box.oauth.allowed-redirect-origins");
        }
    }

    private static boolean containsLocalHost(String value) {
        String lowered = value.toLowerCase();
        return lowered.contains("localhost") || lowered.contains("127.0.0.1");
    }

    private static boolean isNonProductionProfile(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return false;
        }
        return Arrays.stream(activeProfiles)
                .map(profile -> profile == null ? "" : profile.trim().toLowerCase())
                .allMatch(NON_PRODUCTION_PROFILES::contains);
    }
}
