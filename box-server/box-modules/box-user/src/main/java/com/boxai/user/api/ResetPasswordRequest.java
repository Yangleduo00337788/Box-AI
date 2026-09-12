package com.boxai.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Email @NotBlank String email,
        @NotBlank String verificationCode,
        @NotBlank @Size(min = 8, max = 64) String newPassword
) {}
