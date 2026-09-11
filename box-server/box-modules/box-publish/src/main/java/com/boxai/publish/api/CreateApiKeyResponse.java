package com.boxai.publish.api;

public record CreateApiKeyResponse(
        Long id,
        String name,
        String apiKey
) {
}
