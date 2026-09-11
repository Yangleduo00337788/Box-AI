package com.boxai.model.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCredentialRequest(
        @NotNull Long providerId,
        @NotBlank String credentialName,
        @NotBlank String apiKey
) {}
