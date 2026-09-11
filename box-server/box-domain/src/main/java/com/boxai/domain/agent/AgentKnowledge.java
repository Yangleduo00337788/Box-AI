package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AgentKnowledge {

    private Long id;
    private Long agentId;
    private Long versionId;
    private Long knowledgeBaseId;
    private Integer topK;
    private BigDecimal scoreThreshold;
    private String retrievalMode;
    private Boolean rerankEnabled;
    private Boolean citationEnabled;
    private LocalDateTime createdAt;
}
