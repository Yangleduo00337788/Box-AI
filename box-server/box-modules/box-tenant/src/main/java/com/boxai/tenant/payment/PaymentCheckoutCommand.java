package com.boxai.tenant.payment;

import java.math.BigDecimal;

public record PaymentCheckoutCommand(
        Long paymentId,
        Long tenantId,
        Long invoiceId,
        String invoiceNo,
        BigDecimal amount,
        String currency,
        String subject
) {
}
