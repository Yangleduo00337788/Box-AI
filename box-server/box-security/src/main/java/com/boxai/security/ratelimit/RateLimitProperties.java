package com.boxai.security.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.rate-limit")
public class RateLimitProperties {

    /**
     * When false, Redis API rate limits are skipped (load tests). Keep true in production.
     */
    private boolean enabled = true;

    private int loginPerMinute = 20;
    private int chatPerMinute = 60;
    private int publishedChatPerMinute = 60;
    private int apiKeyPerMinute = 120;
    private int toolExecPerMinute = 30;
    private int webhookPerMinute = 60;

    int limitFor(String scope, int fallback) {
        int configured = switch (scope == null ? "" : scope) {
            case "login" -> loginPerMinute;
            case "chat" -> chatPerMinute;
            case "published-chat" -> publishedChatPerMinute;
            case "api-key" -> apiKeyPerMinute;
            case "tool-exec" -> toolExecPerMinute;
            case "workflow-webhook" -> webhookPerMinute;
            default -> fallback;
        };
        return configured;
    }
}
