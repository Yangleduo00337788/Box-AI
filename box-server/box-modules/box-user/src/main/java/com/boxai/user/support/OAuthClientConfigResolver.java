package com.boxai.user.support;

import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.boxai.user.config.OAuthProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OAuthClientConfigResolver {

    private static final List<String> CONFIG_KEYS = List.of(
            "oauth.github.enabled",
            "oauth.github.client_id",
            "oauth.github.client_secret",
            "oauth.google.enabled",
            "oauth.google.client_id",
            "oauth.google.client_secret",
            "oauth.wechat.enabled",
            "oauth.wechat.client_id",
            "oauth.wechat.client_secret");

    private final SystemConfigRepository systemConfigRepository;
    private final OAuthProperties oauthProperties;

    public OAuthClientConfigResolver(SystemConfigRepository systemConfigRepository,
                                     OAuthProperties oauthProperties) {
        this.systemConfigRepository = systemConfigRepository;
        this.oauthProperties = oauthProperties;
    }

    public ResolvedOAuthClient resolve(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        Map<String, String> values = systemConfigRepository.findByKeys(CONFIG_KEYS).stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue, (a, b) -> a));
        OAuthProperties.Provider yaml = yamlProvider(normalized);
        String clientId = firstNonBlank(
                values.get("oauth." + normalized + ".client_id"),
                yaml == null ? null : yaml.getClientId());
        String clientSecret = firstNonBlank(
                values.get("oauth." + normalized + ".client_secret"),
                yaml == null ? null : yaml.getClientSecret());
        boolean configured = clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
        boolean enabledFlag = isEnabled(values, normalized, yaml, configured);
        return new ResolvedOAuthClient(normalized, enabledFlag && configured, clientId, clientSecret);
    }

    public String callbackBaseUrl() {
        String base = oauthProperties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return "http://localhost:8080";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base.trim();
    }

    private OAuthProperties.Provider yamlProvider(String provider) {
        if ("github".equals(provider)) {
            return oauthProperties.getGithub();
        }
        if ("google".equals(provider)) {
            return oauthProperties.getGoogle();
        }
        if ("wechat".equals(provider)) {
            return oauthProperties.getWechat();
        }
        return null;
    }

    private static boolean isEnabled(Map<String, String> values,
                                     String provider,
                                     OAuthProperties.Provider yaml,
                                     boolean configured) {
        String key = "oauth." + provider + ".enabled";
        String fromDb = values.get(key);
        if (fromDb != null && !fromDb.isBlank()) {
            String trimmed = fromDb.trim();
            if ("false".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) {
                return false;
            }
            return "true".equalsIgnoreCase(trimmed) || "1".equals(trimmed);
        }
        if (yaml != null && yaml.isEnabled()) {
            return true;
        }
        return configured;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    public record ResolvedOAuthClient(
            String provider,
            boolean enabled,
            String clientId,
            String clientSecret
    ) {
    }
}
