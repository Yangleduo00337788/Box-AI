package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("tool_database_config")
public class ToolDatabaseConfigDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("tool_id")
    private Long toolId;
    @Column("database_type")
    private String databaseType;
    private String host;
    private Integer port;
    @Column("database_name")
    private String databaseName;
    private String username;
    @Column("password_ciphertext")
    private String passwordCiphertext;
    @Column("allowed_operations")
    private String allowedOperations;
    @Column("max_rows")
    private Integer maxRows;
    @Column("timeout_ms")
    private Integer timeoutMs;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
