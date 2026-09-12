package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("audit_log")
public class AuditLogDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("user_id")
    private Long userId;
    private String action;
    @Column("resource_type")
    private String resourceType;
    @Column("resource_id")
    private String resourceId;
    @Column("resource_name")
    private String resourceName;
    private String result;
    @Column("ip_address")
    private String ipAddress;
    @Column("trace_id")
    private String traceId;
    private String detail;
    @Column("created_at")
    private LocalDateTime createdAt;
}
