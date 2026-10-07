package com.boxai.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberPasswordRequest(
        @NotBlank @Size(min = 8, max = 64) String password
) {}
