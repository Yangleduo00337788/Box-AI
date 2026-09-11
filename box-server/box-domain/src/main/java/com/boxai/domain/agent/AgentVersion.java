package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AgentVersion {

    private Long id;
    private Long agentId;
    private Integer versionNo;
    private String versionName;
    private String status;
    private String systemPrompt;
    private Long modelId;
    private Long platformModelId;
    private String modelSource;
    private BigDecimal temperature;
    private BigDecimal topP;
    private Integer maxTokens;
    private Boolean streamEnabled;
    private Boolean memoryEnabled;
    private Boolean knowledgeEnabled;
    private Boolean toolEnabled;
    private String configJson;
    private LocalDateTime publishedAt;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
