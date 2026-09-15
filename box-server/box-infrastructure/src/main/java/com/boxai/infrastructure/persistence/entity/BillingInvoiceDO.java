package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("billing_invoice")
public class BillingInvoiceDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("tenant_id")
    private Long tenantId;
    @Column("subscription_id")
    private Long subscriptionId;
    @Column("invoice_no")
    private String invoiceNo;
    private String period;
    @Column("plan_id")
    private Long planId;
    @Column("plan_name")
    private String planName;
    private BigDecimal subtotal;
    @Column("overage_amount")
    private BigDecimal overageAmount;
    @Column("total_amount")
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    @Column("paid_at")
    private LocalDateTime paidAt;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
