package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("tool_http_config")
public class ToolHttpConfigDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("tool_id")
    private Long toolId;
    private String method;
    private String url;
    private String headers;
    @Column("query_params")
    private String queryParams;
    @Column("body_type")
    private String bodyType;
    @Column("body_template")
    private String bodyTemplate;
    @Column("timeout_ms")
    private Integer timeoutMs;
    @Column("allow_redirect")
    private Integer allowRedirect;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
