package com.boxai.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrgIdRequest(@NotBlank @Size(max = 64) String orgId) {
}
