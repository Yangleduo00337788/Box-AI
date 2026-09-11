package com.boxai.domain.tool;

import java.util.Optional;

public interface ToolHttpConfigRepository {

    ToolHttpConfig save(ToolHttpConfig config);

    void update(ToolHttpConfig config);

    Optional<ToolHttpConfig> findByToolId(Long toolId);

    void deleteByToolId(Long toolId);
}
