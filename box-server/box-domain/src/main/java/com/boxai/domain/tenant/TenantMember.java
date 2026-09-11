package com.boxai.domain.tenant;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TenantMember {

    private Long id;
    private Long tenantId;
    private Long userId;
    private String roleCode;
    private Integer status;
    private LocalDateTime joinedAt;
}
