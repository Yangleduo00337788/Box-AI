package com.boxai.tenant.payment;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String channel() {
        return "MOCK";
    }

    @Override
    public boolean supports(PaymentProperties properties) {
        String provider = properties.getProvider() == null ? "mock" : properties.getProvider().trim().toLowerCase();
        if ("mock".equals(provider)) {
            return true;
        }
        return !properties.isEnabled();
    }

    @Override
    public PaymentCheckoutResult createCheckout(PaymentCheckoutCommand command, PaymentProperties properties) {
        return new PaymentCheckoutResult(channel(), null, "MOCK-PENDING-" + command.paymentId(), true);
    }

    @Override
    public Optional<Long> resolvePaymentIdFromWebhook(String rawBody, Map<String, String> headers, PaymentProperties properties) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> resolvePaymentIdFromNotify(Map<String, String> params, PaymentProperties properties) {
        return Optional.empty();
    }
}
