package com.boxai.tenant.payment;

import java.util.Map;
import java.util.Optional;

public interface PaymentGateway {

    String channel();

    boolean supports(PaymentProperties properties);

    PaymentCheckoutResult createCheckout(PaymentCheckoutCommand command, PaymentProperties properties);

    Optional<Long> resolvePaymentIdFromWebhook(String rawBody, Map<String, String> headers, PaymentProperties properties);

    Optional<Long> resolvePaymentIdFromNotify(Map<String, String> params, PaymentProperties properties);
}
