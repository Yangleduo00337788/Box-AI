package com.boxai.model.api;

public record CredentialVO(
        Long id,
        Long providerId,
        String providerName,
        String credentialName,
        String maskedApiKey,
        Integer status
) {}
