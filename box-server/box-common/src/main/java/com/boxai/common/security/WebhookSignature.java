package com.boxai.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class WebhookSignature {

    private static final String PREFIX = "sha256=";

    private WebhookSignature() {
    }

    public static String sign(String secret, String payload) {
        if (secret == null || secret.isBlank() || payload == null) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Webhook signature failed", e);
        }
    }

    public static boolean verify(String secret, String payload, String signatureHeader) {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        String expected = sign(secret, payload == null ? "" : payload);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.trim().getBytes(StandardCharsets.UTF_8));
    }
}
