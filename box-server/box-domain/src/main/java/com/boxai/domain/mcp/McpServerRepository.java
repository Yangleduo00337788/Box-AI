package com.boxai.domain.mcp;

import java.util.List;
import java.util.Optional;

public interface McpServerRepository {

    McpServer save(McpServer server);

    void update(McpServer server);

    void delete(Long id);

    Optional<McpServer> findById(Long id);

    List<McpServer> listByWorkspace(Long workspaceId);
}
