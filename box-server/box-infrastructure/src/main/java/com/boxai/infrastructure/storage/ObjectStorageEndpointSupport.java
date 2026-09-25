package com.boxai.infrastructure.storage;

import java.net.URI;

/**
 * MinIO / S3 SDK 要求 endpoint 仅含协议与主机，桶名须单独配置。
 */
public final class ObjectStorageEndpointSupport {

    private ObjectStorageEndpointSupport() {
    }

    public record Parsed(String endpoint, String bucketFromPath) {
    }

    /**
     * @param rawEndpoint 用户输入的 Endpoint，可能误带 /bucket
     * @param currentBucket 已配置的桶名，非空时忽略路径中的桶名
     */
    public static Parsed parse(String rawEndpoint, String currentBucket) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return new Parsed("", trim(currentBucket));
        }
        String input = rawEndpoint.trim();
        if (!input.contains("://")) {
            input = "https://" + input;
        }
        try {
            URI uri = URI.create(input);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return new Parsed(stripTrailingSlash(rawEndpoint.trim()), trim(currentBucket));
            }
            StringBuilder base = new StringBuilder();
            base.append(uri.getScheme() == null ? "https" : uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() > 0) {
                base.append(':').append(uri.getPort());
            }
            String endpoint = base.toString();
            String bucket = trim(currentBucket);
            String path = uri.getPath();
            if (path != null && !path.isBlank() && !"/".equals(path)) {
                String segment = path.startsWith("/") ? path.substring(1) : path;
                int slash = segment.indexOf('/');
                if (slash >= 0) {
                    segment = segment.substring(0, slash);
                }
                segment = segment.trim();
                if (!segment.isEmpty() && bucket.isEmpty()) {
                    bucket = segment;
                }
            }
            return new Parsed(endpoint, bucket);
        } catch (Exception e) {
            return new Parsed(stripTrailingSlash(rawEndpoint.trim()), trim(currentBucket));
        }
    }

    public static String normalizeEndpointOnly(String rawEndpoint) {
        return parse(rawEndpoint, "ignored").endpoint();
    }

    private static String stripTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
