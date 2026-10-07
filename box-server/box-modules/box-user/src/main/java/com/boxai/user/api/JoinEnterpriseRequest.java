package com.boxai.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinEnterpriseRequest(
        @NotBlank String orgId,
        @NotBlank String inviteCode,
        @NotBlank @Size(min = 2, max = 64) String account,
        @NotBlank @Size(min = 8, max = 64) String password,
        String nickname
) {}
