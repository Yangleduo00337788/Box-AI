package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentWorkflowRepository {

    AgentWorkflow save(AgentWorkflow binding);

    void update(AgentWorkflow binding);

    void delete(Long id);

    Optional<AgentWorkflow> findByVersionAndWorkflow(Long versionId, Long workflowId);

    List<AgentWorkflow> listByVersionId(Long versionId);

    Optional<AgentWorkflow> findDefaultByVersionId(Long versionId);

    void clearDefaultForVersion(Long versionId);
}
