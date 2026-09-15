package com.boxai.domain.billing;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentRecord {

    private Long id;
    private Long tenantId;
    private Long invoiceId;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private String status;
    private String externalRef;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
