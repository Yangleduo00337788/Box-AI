package com.boxai.domain.tool;

import java.util.List;
import java.util.Optional;

public interface ToolRepository {

    Tool save(Tool tool);

    void update(Tool tool);

    Optional<Tool> findById(Long id);

    List<Tool> listByWorkspace(Long workspaceId);

    void delete(Long id);
}
