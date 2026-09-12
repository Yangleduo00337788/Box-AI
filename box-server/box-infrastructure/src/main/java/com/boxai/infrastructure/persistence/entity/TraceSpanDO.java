package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("trace_span")
public class TraceSpanDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("trace_id")
    private String traceId;
    @Column("span_id")
    private String spanId;
    @Column("parent_span_id")
    private String parentSpanId;
    @Column("span_type")
    private String spanType;
    private String name;
    private String status;
    private String input;
    private String output;
    @Column("model_id")
    private Long modelId;
    @Column("tool_id")
    private Long toolId;
    @Column("input_tokens")
    private Integer inputTokens;
    @Column("output_tokens")
    private Integer outputTokens;
    @Column("start_time")
    private LocalDateTime startTime;
    @Column("end_time")
    private LocalDateTime endTime;
    @Column("duration_ms")
    private Long durationMs;
    @Column("error_code")
    private String errorCode;
    @Column("error_message")
    private String errorMessage;
    private String metadata;
    @Column("created_at")
    private LocalDateTime createdAt;
}
