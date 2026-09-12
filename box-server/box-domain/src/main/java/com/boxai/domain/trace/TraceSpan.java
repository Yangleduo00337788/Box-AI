package com.boxai.domain.trace;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TraceSpan {

    private Long id;
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String spanType;
    private String name;
    private String status;
    private String inputJson;
    private String outputJson;
    private Long modelId;
    private Long toolId;
    private Integer inputTokens;
    private Integer outputTokens;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorCode;
    private String errorMessage;
    private String metadataJson;
    private LocalDateTime createdAt;
}
