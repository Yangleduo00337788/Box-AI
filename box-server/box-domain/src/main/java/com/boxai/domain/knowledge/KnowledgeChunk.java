package com.boxai.domain.knowledge;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class KnowledgeChunk {

    private Long id;
    private Long workspaceId;
    private Long knowledgeBaseId;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private Integer pageNumber;
    private String metadataJson;
    private String esDocumentId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
