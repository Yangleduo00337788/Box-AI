package com.boxai.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationCodeRequest(
        @Email @NotBlank String email,
        @NotBlank String purpose
) {}
