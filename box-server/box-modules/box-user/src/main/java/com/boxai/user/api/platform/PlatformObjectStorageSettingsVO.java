package com.boxai.user.api.platform;

public record PlatformObjectStorageSettingsVO(
        String activeBackend,
        String effectiveBucket,
        PlatformObjectStorageBackendVO minio,
        PlatformObjectStorageBackendVO r2,
        String resolutionHint
) {
}
