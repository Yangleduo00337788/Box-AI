package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("trace")
public class TraceDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("trace_id")
    private String traceId;
    @Column("execution_id")
    private Long executionId;
    @Column("workspace_id")
    private Long workspaceId;
    private String name;
    private String status;
    @Column("start_time")
    private LocalDateTime startTime;
    @Column("end_time")
    private LocalDateTime endTime;
    @Column("duration_ms")
    private Long durationMs;
    private String metadata;
    @Column("created_at")
    private LocalDateTime createdAt;
}
