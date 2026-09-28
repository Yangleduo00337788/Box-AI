package com.boxai.tenant.api;

import java.math.BigDecimal;
import java.util.Map;

public record CreateSubscriptionOrderVO(
        Long subscriptionId,
        Long invoiceId,
        Long paymentId,
        String invoiceNo,
        BigDecimal amount,
        String currency,
        String paymentStatus,
        String paymentChannel,
        String paymentUrl,
        boolean requiresClientConfirm,
        String checkoutFormAction,
        Map<String, String> checkoutForm
) {
}
