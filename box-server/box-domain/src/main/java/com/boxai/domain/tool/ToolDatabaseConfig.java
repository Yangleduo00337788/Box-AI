package com.boxai.domain.tool;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ToolDatabaseConfig {

    private Long id;
    private Long toolId;
    private String databaseType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String passwordCiphertext;
    private String allowedOperationsJson;
    private Integer maxRows;
    private Integer timeoutMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
