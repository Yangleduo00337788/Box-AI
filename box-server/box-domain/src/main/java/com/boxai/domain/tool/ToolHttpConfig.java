package com.boxai.domain.tool;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ToolHttpConfig {

    private Long id;
    private Long toolId;
    private String method;
    private String url;
    private String headersJson;
    private String queryParamsJson;
    private String bodyType;
    private String bodyTemplate;
    private Integer timeoutMs;
    private Boolean allowRedirect;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
