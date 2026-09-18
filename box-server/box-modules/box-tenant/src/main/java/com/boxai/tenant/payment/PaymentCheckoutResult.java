package com.boxai.tenant.payment;

public record PaymentCheckoutResult(
        String channel,
        String paymentUrl,
        String externalRef,
        boolean requiresClientConfirm
) {
}
