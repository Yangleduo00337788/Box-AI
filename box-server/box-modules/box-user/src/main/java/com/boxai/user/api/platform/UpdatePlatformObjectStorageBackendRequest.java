package com.boxai.user.api.platform;

public record UpdatePlatformObjectStorageBackendRequest(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        Boolean useYamlFallback
) {
}
