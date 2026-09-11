package com.boxai.user.api;

import jakarta.validation.constraints.Pattern;

public record UpdateUserPreferenceRequest(
        @Pattern(regexp = "light|dark|system") String theme,
        Boolean sendWithEnter
) {}
