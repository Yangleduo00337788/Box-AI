package com.boxai.tenant.payment;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

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
}
