package com.boxai.infrastructure.ai;

/**
 * Normalizes OpenAI-compatible provider base URLs (SiliconFlow, OpenAI, etc.).
 */
public final class OpenAiCompatibleApiUrls {

    private OpenAiCompatibleApiUrls() {
    }

    public static String normalizeChatBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null || baseUrl.isBlank()
                ? "https://api.openai.com/v1"
                : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/chat/completions")) {
            trimmed = trimmed.substring(0, trimmed.length() - "/chat/completions".length());
        }
        if (!trimmed.endsWith("/v1")) {
            trimmed = trimmed + "/v1";
        }
        return trimmed;
    }

    public static String chatCompletionsUrl(String baseUrl) {
        return normalizeChatBaseUrl(baseUrl) + "/chat/completions";
    }
}
