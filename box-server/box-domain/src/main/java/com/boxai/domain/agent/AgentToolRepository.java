package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentToolRepository {

    AgentTool save(AgentTool binding);

    void update(AgentTool binding);

    void delete(Long id);

    Optional<AgentTool> findByVersionAndTool(Long versionId, Long toolId);

    List<AgentTool> listByVersionId(Long versionId);

    int countByToolId(Long toolId);

    List<Long> listDistinctAgentIdsByToolId(Long toolId);
}
