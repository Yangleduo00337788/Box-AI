package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentLongTermMemoryRepository {

    Optional<AgentLongTermMemory> findById(Long id);

    List<AgentLongTermMemory> listByIds(List<Long> ids);

    List<AgentLongTermMemory> listByAgentAndUser(Long agentId, Long userId);

    AgentLongTermMemory save(AgentLongTermMemory memory);

    void delete(Long id);
}
