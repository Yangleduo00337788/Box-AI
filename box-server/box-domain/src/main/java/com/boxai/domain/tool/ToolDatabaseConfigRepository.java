package com.boxai.domain.tool;

import java.util.Optional;

public interface ToolDatabaseConfigRepository {

    ToolDatabaseConfig save(ToolDatabaseConfig config);

    void update(ToolDatabaseConfig config);

    Optional<ToolDatabaseConfig> findByToolId(Long toolId);

    void deleteByToolId(Long toolId);
}
