package com.boxai.user.api.platform;

import jakarta.validation.constraints.NotBlank;

public record UpdatePlatformObjectStorageRequest(
        @NotBlank String activeBackend,
        UpdatePlatformObjectStorageBackendRequest minio,
        UpdatePlatformObjectStorageBackendRequest r2
) {
}
