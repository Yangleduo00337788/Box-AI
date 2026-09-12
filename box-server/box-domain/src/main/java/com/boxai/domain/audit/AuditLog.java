package com.boxai.domain.audit;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AuditLog {

    private Long id;
    private Long workspaceId;
    private Long userId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String resourceName;
    private String result;
    private String ipAddress;
    private String traceId;
    private String detail;
    private LocalDateTime createdAt;
}
