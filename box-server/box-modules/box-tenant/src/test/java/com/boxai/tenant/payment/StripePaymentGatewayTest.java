package com.boxai.tenant.payment;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StripePaymentGatewayTest {

    private final StripePaymentGateway gateway = new StripePaymentGateway();

    @Test
    void rejectsWebhookWithoutSecret() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("stripe");
        properties.getStripe().setWebhookSecret("");

        Optional<Long> paymentId = gateway.resolvePaymentIdFromWebhook(
                "{\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"metadata\":{\"payment_id\":\"1\"}}}}",
                Map.of("Stripe-Signature", "t=1,v1=deadbeef"),
                properties);

        assertTrue(paymentId.isEmpty());
        assertFalse(gateway.isWebhookAuthentic(
                "{\"type\":\"checkout.session.completed\"}",
                Map.of("Stripe-Signature", "t=1,v1=deadbeef"),
                properties));
    }

    @Test
    void rejectsWebhookWithoutSignature() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("stripe");
        properties.getStripe().setWebhookSecret("whsec_test");

        Optional<Long> paymentId = gateway.resolvePaymentIdFromWebhook(
                "{\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"metadata\":{\"payment_id\":\"1\"}}}}",
                Map.of(),
                properties);

        assertTrue(paymentId.isEmpty());
    }

    @Test
    void acceptsSignedCheckoutCompletedWebhook() throws Exception {
        String secret = "whsec_test";
        String payload = "{\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"payment_status\":\"paid\",\"metadata\":{\"payment_id\":\"88\"}}}}";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) {
            hex.append(String.format("%02x", value));
        }
        PaymentProperties properties = new PaymentProperties();
        properties.getStripe().setWebhookSecret(secret);

        Optional<Long> paymentId = gateway.resolvePaymentIdFromWebhook(
                payload,
                Map.of("Stripe-Signature", "t=" + timestamp + ",v1=" + hex),
                properties);

        assertEquals(Optional.of(88L), paymentId);
    }

    @Test
    void convertsZeroDecimalCurrencyWithoutCents() {
        assertEquals(9900L, StripePaymentGateway.toStripeAmount(new BigDecimal("99.00"), "usd"));
        assertEquals(99L, StripePaymentGateway.toStripeAmount(new BigDecimal("99"), "jpy"));
    }

    @Test
    void skipsQueryWhenExternalRefIsNotStripeSession() {
        PaymentProperties properties = new PaymentProperties();
        properties.getStripe().setSecretKey("sk_test");
        assertTrue(gateway.queryPaidExternalRef("BOX-PAY-8", 8L, properties).isEmpty());
    }
}
