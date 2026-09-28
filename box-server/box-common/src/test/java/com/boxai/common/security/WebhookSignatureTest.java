package com.boxai.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureTest {

    @Test
    void verifyRejectsBlankSecret() {
        assertFalse(WebhookSignature.verify(null, "{}", "sha256=abc"));
        assertFalse(WebhookSignature.verify("", "{}", "sha256=abc"));
        assertFalse(WebhookSignature.verify("   ", "{}", "sha256=abc"));
    }

    @Test
    void verifyAcceptsValidSignature() {
        String payload = "{\"foo\":1}";
        String signature = WebhookSignature.sign("secret", payload);
        assertTrue(WebhookSignature.verify("secret", payload, signature));
    }

    @Test
    void verifyRejectsInvalidSignature() {
        assertFalse(WebhookSignature.verify("secret", "{}", "sha256=deadbeef"));
    }
}
