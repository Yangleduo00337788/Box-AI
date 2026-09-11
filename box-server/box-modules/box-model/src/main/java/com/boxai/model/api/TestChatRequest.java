package com.boxai.model.api;

import jakarta.validation.constraints.NotBlank;

public record TestChatRequest(
        @NotBlank String message
) {}
