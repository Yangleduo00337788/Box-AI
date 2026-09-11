package com.boxai.domain.plan;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TenantUsage {

    private Long id;
    private Long tenantId;
    private String period;
    private Integer aiCalls;
    private Long tokens;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
