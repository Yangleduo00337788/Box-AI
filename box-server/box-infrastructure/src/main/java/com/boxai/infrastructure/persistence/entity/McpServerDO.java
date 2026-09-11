package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("mcp_server")
public class McpServerDO {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    private String name;
    @Column("server_key")
    private String serverKey;
    private String description;
    @Column("transport_type")
    private String transportType;
    @Column("endpoint_url")
    private String endpointUrl;
    @Column("auth_type")
    private String authType;
    @Column("auth_config")
    private String authConfig;
    @Column("tool_catalog_json")
    private String toolCatalogJson;
    private Integer status;
    @Column("last_sync_at")
    private LocalDateTime lastSyncAt;
    @Column("created_by")
    private Long createdBy;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    private Integer deleted;
}
