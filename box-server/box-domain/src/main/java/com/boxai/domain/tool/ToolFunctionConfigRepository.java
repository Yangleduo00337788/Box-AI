package com.boxai.domain.tool;

import java.util.Optional;

public interface ToolFunctionConfigRepository {

    ToolFunctionConfig save(ToolFunctionConfig config);

    void update(ToolFunctionConfig config);

    Optional<ToolFunctionConfig> findByToolId(Long toolId);

    void deleteByToolId(Long toolId);
}
