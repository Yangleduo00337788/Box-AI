package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentVersionRepository {

    AgentVersion save(AgentVersion version);

    void update(AgentVersion version);

    Optional<AgentVersion> findById(Long id);

    Optional<AgentVersion> findLatestDraft(Long agentId);

    Optional<AgentVersion> findByAgentIdAndVersionNo(Long agentId, Integer versionNo);

    int maxVersionNo(Long agentId);

    List<AgentVersion> listByAgentId(Long agentId);
}
