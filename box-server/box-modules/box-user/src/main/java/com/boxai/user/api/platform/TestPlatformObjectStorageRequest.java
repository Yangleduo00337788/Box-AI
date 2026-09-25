package com.boxai.user.api.platform;

import jakarta.validation.constraints.NotBlank;

public record TestPlatformObjectStorageRequest(@NotBlank String backend) {
}
