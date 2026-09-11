package com.boxai.publish.support;

import com.boxai.common.constant.ApiKeyPrefixes;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return ApiKeyPrefixes.LIVE + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String prefix(String apiKey) {
        if (apiKey == null || apiKey.length() <= 16) {
            return apiKey;
        }
        return apiKey.substring(0, 16);
    }
}
