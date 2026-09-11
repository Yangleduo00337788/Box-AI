package com.boxai.model.api.platform;

import java.time.LocalDateTime;

public record PlatformCredentialVO(
        Long id,
        Long providerId,
        String credentialName,
        String apiKeyMasked,
        Integer status,
        LocalDateTime lastUsedAt
) {}
