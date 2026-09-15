package com.boxai.domain.billing;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class Subscription {

    private Long id;
    private Long tenantId;
    private Long planId;
    private String status;
    private String billingCycle;
    private LocalDate currentPeriodStart;
    private LocalDate currentPeriodEnd;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
