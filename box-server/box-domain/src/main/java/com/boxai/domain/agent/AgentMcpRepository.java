package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentMcpRepository {

    AgentMcp save(AgentMcp binding);

    void delete(Long id);

    Optional<AgentMcp> findByVersionAndMcpServer(Long versionId, Long mcpServerId);

    List<AgentMcp> listByVersionId(Long versionId);
}
