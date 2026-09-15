package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("payment_record")
public class PaymentRecordDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("tenant_id")
    private Long tenantId;
    @Column("invoice_id")
    private Long invoiceId;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private String status;
    @Column("external_ref")
    private String externalRef;
    @Column("paid_at")
    private LocalDateTime paidAt;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
