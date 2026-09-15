package com.boxai.domain.billing;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BillingInvoice {

    private Long id;
    private Long tenantId;
    private Long subscriptionId;
    private String invoiceNo;
    private String period;
    private Long planId;
    private String planName;
    private BigDecimal subtotal;
    private BigDecimal overageAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
