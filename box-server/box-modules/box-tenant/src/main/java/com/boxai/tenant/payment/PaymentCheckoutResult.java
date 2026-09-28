package com.boxai.tenant.payment;

import java.util.Map;

public record PaymentCheckoutResult(
        String channel,
        String paymentUrl,
        String externalRef,
        boolean requiresClientConfirm,
        String checkoutFormAction,
        Map<String, String> checkoutForm
) {
    public PaymentCheckoutResult(String channel, String paymentUrl, String externalRef, boolean requiresClientConfirm) {
        this(channel, paymentUrl, externalRef, requiresClientConfirm, null, Map.of());
    }
}
