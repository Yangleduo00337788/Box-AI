package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentRepository {

    Agent save(Agent agent);

    void update(Agent agent);

    Optional<Agent> findById(Long id);

    List<Agent> listByWorkspace(Long workspaceId);

    List<Agent> searchByName(Long workspaceId, String keyword, int limit);

    int countByWorkspace(Long workspaceId);

    void delete(Long id);
}
