package com.boxai.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpgradeEnterpriseRequest(@NotBlank @Size(max = 128) String companyName) {
}
