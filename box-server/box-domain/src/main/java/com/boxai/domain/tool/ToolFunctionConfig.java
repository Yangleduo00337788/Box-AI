package com.boxai.domain.tool;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ToolFunctionConfig {

    private Long id;
    private Long toolId;
    private String functionName;
    private String functionCode;
    private String runtime;
    private Integer timeoutMs;
    private Integer memoryLimitMb;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
