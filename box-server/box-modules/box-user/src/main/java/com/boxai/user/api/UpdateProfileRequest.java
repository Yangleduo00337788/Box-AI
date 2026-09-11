package com.boxai.user.api;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 64) String nickname,
        @Size(max = 255) String bio
) {}
