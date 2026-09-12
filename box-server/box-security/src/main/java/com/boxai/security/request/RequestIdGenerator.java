package com.boxai.security.request;

import java.util.UUID;

public final class RequestIdGenerator {

    private static final int MAX_LENGTH = 64;

    private RequestIdGenerator() {
    }

    public static String generate() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String resolve(String incoming) {
        if (incoming == null) {
            return generate();
        }
        String trimmed = incoming.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            return generate();
        }
        return trimmed;
    }
}
