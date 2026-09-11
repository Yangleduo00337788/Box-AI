package com.boxai.domain.knowledge;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class KnowledgeBase {

    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private String icon;
    private Long embeddingModelId;
    private Long rerankModelId;
    private String chunkConfigJson;
    private String retrievalConfigJson;
    private Integer documentCount;
    private Long chunkCount;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
