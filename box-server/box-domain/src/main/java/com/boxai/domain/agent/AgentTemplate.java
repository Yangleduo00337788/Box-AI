package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AgentTemplate {

    private Long id;
    private String templateCode;
    private String name;
    private String description;
    private String avatarUrl;
    private String category;
    private String systemPrompt;
    private Long platformModelId;
    private BigDecimal temperature;
    private BigDecimal topP;
    private Integer maxTokens;
    private Boolean streamEnabled;
    private String status;
    private String reviewStatus;
    private String visibility;
    private String tenantIdsJson;
    private Integer rolloutPercent;
    private Integer sortOrder;
    private Integer installCount;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
