package com.boxai.domain.audit;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AuditLogQuery {

    private Long workspaceId;
    private String action;
    private String resourceType;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int page = 1;
    private int pageSize = 20;
}
