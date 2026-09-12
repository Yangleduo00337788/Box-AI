package com.boxai.domain.workflow;

import java.util.Optional;

public interface WorkflowVersionRepository {

    WorkflowVersion save(WorkflowVersion version);

    void update(WorkflowVersion version);

    Optional<WorkflowVersion> findById(Long id);

    Optional<WorkflowVersion> findLatestDraft(Long workflowId);

    Optional<Integer> findMaxVersionNo(Long workflowId);

    void deleteByWorkflowId(Long workflowId);

    int countSubWorkflowReferences(Long workflowId, Long workspaceId);
}
