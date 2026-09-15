package com.boxai.tenant.api;

import java.math.BigDecimal;

public record CreateSubscriptionOrderVO(
        Long subscriptionId,
        Long invoiceId,
        Long paymentId,
        String invoiceNo,
        BigDecimal amount,
        String currency,
        String paymentStatus
) {
}
