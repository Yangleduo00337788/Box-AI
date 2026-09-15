package com.boxai.tenant.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillingInvoiceVO(
        Long id,
        Long tenantId,
        String tenantName,
        String invoiceNo,
        String period,
        String planName,
        BigDecimal subtotal,
        BigDecimal overageAmount,
        BigDecimal totalAmount,
        String currency,
        String status,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
