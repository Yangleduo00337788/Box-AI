package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentSubAgentRepository {

    AgentSubAgent save(AgentSubAgent binding);

    void delete(Long id);

    Optional<AgentSubAgent> findByVersionAndSubAgent(Long versionId, Long subAgentId);

    List<AgentSubAgent> listByVersionId(Long versionId);

    int countBySubAgentId(Long subAgentId);
}
