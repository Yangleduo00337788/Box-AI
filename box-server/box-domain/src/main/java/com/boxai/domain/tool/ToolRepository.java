package com.boxai.domain.tool;

import java.util.List;
import java.util.Optional;

public interface ToolRepository {

    Tool save(Tool tool);

    void update(Tool tool);

    Optional<Tool> findById(Long id);

    List<Tool> listByWorkspace(Long workspaceId);

    List<Tool> searchByName(Long workspaceId, String keyword, int limit);

    void delete(Long id);
}
