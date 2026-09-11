package com.boxai.domain.trace;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Trace {

    private Long id;
    private String traceId;
    private Long executionId;
    private Long workspaceId;
    private String name;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String metadataJson;
    private LocalDateTime createdAt;
}
