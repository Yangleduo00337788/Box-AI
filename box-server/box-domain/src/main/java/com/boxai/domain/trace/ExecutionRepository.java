package com.boxai.domain.trace;

import java.util.List;
import java.util.Optional;

public interface ExecutionRepository {

    Execution save(Execution execution);

    void update(Execution execution);

    Optional<Execution> findById(Long id);

    List<Execution> listByWorkspace(Long workspaceId, int limit);

    List<Execution> listByWorkspace(Long workspaceId,
                                      String executionType,
                                      String status,
                                      Long agentId,
                                      Long conversationId,
                                      int limit);

    List<Execution> listByConversation(Long conversationId, int limit);

    int countByWorkspace(Long workspaceId);

    List<Execution> listRecent(int limit);

    int countAll();
}
