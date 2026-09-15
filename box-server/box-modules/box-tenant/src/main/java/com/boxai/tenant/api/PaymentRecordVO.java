package com.boxai.tenant.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRecordVO(
        Long id,
        Long invoiceId,
        BigDecimal amount,
        String currency,
        String channel,
        String status,
        LocalDateTime paidAt
) {
}
