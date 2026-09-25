package com.boxai.infrastructure.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectStorageEndpointSupportTest {

    @Test
    void stripsBucketPathFromR2Endpoint() {
        ObjectStorageEndpointSupport.Parsed parsed = ObjectStorageEndpointSupport.parse(
                "https://463be94a00648b91c79379e11382a055.r2.cloudflarestorage.com/box-ai",
                "");
        assertEquals("https://463be94a00648b91c79379e11382a055.r2.cloudflarestorage.com", parsed.endpoint());
        assertEquals("box-ai", parsed.bucketFromPath());
    }

    @Test
    void keepsExplicitBucketOverPath() {
        ObjectStorageEndpointSupport.Parsed parsed = ObjectStorageEndpointSupport.parse(
                "https://host.example.com/wrong",
                "my-bucket");
        assertEquals("https://host.example.com", parsed.endpoint());
        assertEquals("my-bucket", parsed.bucketFromPath());
    }
}
