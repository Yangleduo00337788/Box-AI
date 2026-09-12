package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("tool_function_config")
public class ToolFunctionConfigDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("tool_id")
    private Long toolId;
    @Column("function_name")
    private String functionName;
    @Column("function_code")
    private String functionCode;
    private String runtime;
    @Column("timeout_ms")
    private Integer timeoutMs;
    @Column("memory_limit_mb")
    private Integer memoryLimitMb;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
