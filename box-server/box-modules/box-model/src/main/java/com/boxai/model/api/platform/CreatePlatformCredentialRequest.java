package com.boxai.model.api.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePlatformCredentialRequest(
        @NotNull Long providerId,
        @NotBlank @Size(max = 128) String credentialName,
        @NotBlank String apiKey
) {}
