package com.boxai.user.api.platform;

public record PlatformObjectStorageBackendVO(
        String endpoint,
        String accessKey,
        boolean hasSecretKey,
        String bucket,
        boolean useYamlFallback,
        boolean configured,
        boolean reachable
) {
}
