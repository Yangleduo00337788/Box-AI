package com.boxai.domain.mcp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class McpServer {
    private Long id;
    private Long workspaceId;
    private String name;
    private String serverKey;
    private String description;
    private String transportType;
    private String endpointUrl;
    private String authType;
    private String authConfigJson;
    private String toolCatalogJson;
    private Integer status;
    private LocalDateTime lastSyncAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
