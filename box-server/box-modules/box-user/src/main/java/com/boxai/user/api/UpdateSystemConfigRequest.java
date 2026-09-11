package com.boxai.user.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateSystemConfigRequest(
        @NotBlank String configKey,
        String configValue,
        String description
) {}
